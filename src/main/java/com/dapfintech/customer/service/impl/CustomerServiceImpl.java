package com.dapfintech.customer.service.impl;
import com.dapfintech.customer.history.enums.CustomerHistoryAction;
import com.dapfintech.customer.history.service.CustomerHistoryService;
import com.dapfintech.customer.address.dto.request.CreateAddressRequest;
import com.dapfintech.customer.address.service.AddressService;
import com.dapfintech.customer.document.service.DocumentService;
import com.dapfintech.customer.dto.response.CustomerDetailsResponse;
import com.dapfintech.customer.guarantor.dto.response.GuarantorResponse;
import com.dapfintech.customer.guarantor.service.GuarantorService;
import com.dapfintech.loan.dto.response.LoanResponse;
import com.dapfintech.loan.dto.response.LoanSummaryResponse;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.loan.service.LoanService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.auth.entity.User;
import com.dapfintech.auth.repository.UserRepository;
import com.dapfintech.customer.dto.request.CreateCustomerRequest;
import com.dapfintech.customer.dto.request.UpdateCustomerRequest;
import com.dapfintech.customer.dto.response.CustomerResponse;
import com.dapfintech.customer.dto.response.DeleteCustomerResponse;
import com.dapfintech.customer.entity.Customer;
import com.dapfintech.customer.mapper.CustomerMapper;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.customer.service.CustomerService;
import com.dapfintech.market.entity.EmployeeMarketAssignment;
import com.dapfintech.market.entity.Market;
import com.dapfintech.market.repository.EmployeeMarketAssignmentRepository;
import com.dapfintech.market.repository.MarketRepository;
import com.dapfintech.security.service.impl.AccessControlServiceImpl;
import com.dapfintech.sync.service.SyncLogService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

import com.dapfintech.customer.dto.request.CustomerFilterRequest;
import com.dapfintech.customer.specification.CustomerSpecification;
import com.dapfintech.customer.dto.request.UpdateCustomerStatusRequest;
import com.dapfintech.customer.enums.AddressType;
import com.dapfintech.customer.enums.CustomerStatus;
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl
        implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final LoanRepository loanRepository;
    private final MarketRepository marketRepository;
    private final AccessControlServiceImpl accessControlService;
    private final UserRepository userRepository;
    private final EmployeeMarketAssignmentRepository assignmentRepository;
    private final AuditLogService auditLogService;
    private final SyncLogService syncLogService;
    private final AddressService addressService;
    private final LoanService loanService;
    private final GuarantorService guarantorService;
    private final CustomerHistoryService customerHistoryService;
    private final DocumentService documentService;
    private static final Set<LoanStatus> BLOCKING_LOAN_STATUSES =
            Set.of(
                    LoanStatus.ACTIVE,
                    LoanStatus.APPROVED,
                    LoanStatus.DISBURSED,
                    LoanStatus.PENDING_APPROVAL);
    
    @Override
    public CustomerResponse updateCustomerStatus(
            UUID customerId,
            UpdateCustomerStatusRequest request
    ) {

        accessControlService.validateCustomerAccess(
                customerId
        );

        Customer customer =
                customerRepository.findById(customerId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Customer not found"
                                )
                        );

        CustomerStatus oldStatus =
                customer.getStatus();

        CustomerStatus newStatus =
                request.getStatus();

        if (oldStatus == newStatus) {

            throw new RuntimeException(
                    "Customer already has status: "
                            + newStatus
            );
        }
        

      

        Customer savedCustomer =
                customerRepository.save(customer);

        String action = switch (newStatus) {

            case ACTIVE ->
                    "ACTIVATE_CUSTOMER";

            case INACTIVE ->
                    "DEACTIVATE_CUSTOMER";

            case SUSPENDED ->
                    "SUSPEND_CUSTOMER";

            case BLACKLISTED ->
                    "BLACKLIST_CUSTOMER";

            case PENDING_VERIFICATION ->
                    "MARK_CUSTOMER_PENDING_VERIFICATION";
        };
        customerHistoryService.recordHistory(

                savedCustomer,

                CustomerHistoryAction.CUSTOMER_STATUS_CHANGED,

                "Customer Status Changed",

                "Customer status was changed from " +
                        oldStatus +
                        " to " +
                        savedCustomer.getStatus() +
                        ".",

                oldStatus.name(),

                savedCustomer.getStatus().name()
        );

        auditLogService.log(
                "SYSTEM",
                action,
                "CUSTOMER",
                savedCustomer.getId().toString()
        );

        syncLogService.logSync(
                "CUSTOMER",
                savedCustomer.getId().toString()
        );

        return customerMapper.toResponse(
                savedCustomer
        );
    }
    
    @Override
    public Page<CustomerResponse> filterCustomers(
            CustomerFilterRequest filter,
            int page,
            int size
    ) {

        //----------------------------------------------------------
        // VALIDATE PAGINATION
        //----------------------------------------------------------

        if (page < 0) {
            throw new RuntimeException(
                    "Page number cannot be negative"
            );
        }

        if (size <= 0 || size > 100) {
            throw new RuntimeException(
                    "Page size must be between 1 and 100"
            );
        }


        //----------------------------------------------------------
        // AUTHENTICATED USER
        //----------------------------------------------------------

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String mobileNumber =
                authentication.getName();

        User user =
                userRepository
                        .findByMobileNumber(
                                mobileNumber
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );


        //----------------------------------------------------------
        // PAGE REQUEST
        //----------------------------------------------------------

        PageRequest pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );


        //----------------------------------------------------------
        // ADMIN
        //----------------------------------------------------------

        if (user.getRole()
                .getRoleName()
                .equalsIgnoreCase("ADMIN")) {

            return customerRepository
                    .findAll(
                            CustomerSpecification
                                    .withFilters(filter),
                            pageable
                    )
                    .map(
                            customerMapper::toResponse
                    );
        }


        //----------------------------------------------------------
        // EMPLOYEE
        //----------------------------------------------------------

        EmployeeMarketAssignment assignment =
                assignmentRepository
                        .findFirstByEmployeeIdAndIsActiveTrue(
                                user.getId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "No market assigned"
                                )
                        );

        CustomerFilterRequest employeeFilter =
                CustomerFilterRequest
                        .builder()

                        .keyword(
                                filter == null
                                        ? null
                                        : filter.getKeyword()
                        )

                        .status(
                                filter == null
                                        ? null
                                        : filter.getStatus()
                        )

                        .marketId(
                                assignment.getMarket().getId()
                        )

                        .build();

        return customerRepository
                .findAll(
                        CustomerSpecification
                                .withFilters(
                                        employeeFilter
                                ),
                        pageable
                )
                .map(
                        customerMapper::toResponse
                );
    }
    
    @Override
    public CustomerDetailsResponse getCustomerDetails(
            UUID customerId
    ) {

        CustomerResponse customer =
                getCustomerById(customerId);

        var addresses =
                addressService.getCustomerAddresses(
                        customerId
                );

        List<LoanResponse> loans =
                loanService.getCustomerLoans(
                        customerId
                );

        LoanResponse loan = null;

        LoanSummaryResponse loanSummary = null;

        if (!loans.isEmpty()) {

            loan = loans.get(0);

            loanSummary =
                    loanService.getLoanSummary(
                            loan.getId()
                    );

        }

        List<GuarantorResponse> guarantors =
                guarantorService
                        .getCustomerGuarantors(
                                customerId
                        );

        GuarantorResponse guarantor =

                guarantors.isEmpty()

                        ? null

                        : guarantors.get(0);

        return CustomerDetailsResponse
                .builder()

                .customer(customer)

                .addresses(addresses)

                .loan(loan)

                .loanSummary(loanSummary)

                .guarantor(guarantor)

                .documents(

                        documentService
                                .getCustomerDocuments(
                                        customerId
                                )

                )

                .build();

    }
    
    
    @Override
    public void deleteCustomer(UUID customerId) {
    	
    	accessControlService
        .validateCustomerAccess(
                customerId
        );

        Customer customer =
                customerRepository.findById(customerId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Customer not found"
                        )
                );
        customerHistoryService.recordHistory(

                customer,

                CustomerHistoryAction.CUSTOMER_DELETED,

                "Customer Deleted",

                "Customer was removed from active customer records.",

                "ACTIVE_RECORD",

                "DELETED"
        );
        customer.setDeleted(true);

        customer.setDeletedAt(
                LocalDateTime.now()
        );

        customerRepository.delete(customer);
    }

    @Override
    public CustomerResponse createCustomer(
            CreateCustomerRequest request
    ) {

        if(customerRepository.existsByMobileNumber(
                request.getMobileNumber()
        )) {

            throw new RuntimeException(
                    "Customer already exists"
            );
        }
        
        
        Customer customer = customerMapper.toEntity(request);

        customer.setCustomerCode(
                generateCustomerCode()
        );

        
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String mobileNumber =
                authentication.getName();

        User employee =
                userRepository
                        .findByMobileNumber(mobileNumber)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Employee not found"
                                )
                        );
        customer.setCreatedBy(employee);
        
        if (employee.getRole().getRoleName().equalsIgnoreCase("ADMIN")) {
            if (request.getMarketId() != null) {
                Market market = marketRepository.findById(request.getMarketId())
                        .orElseThrow(() -> new RuntimeException("Market not found"));
                customer.setMarket(market);
            } else {
                customer.setMarket(null);
            }
        } else {
            if (request.getMarketId() != null) {
                Market market = marketRepository.findById(request.getMarketId())
                        .orElseThrow(() -> new RuntimeException("Market not found"));
                customer.setMarket(market);
            } else {
                EmployeeMarketAssignment assignment =
                        assignmentRepository
                                .findFirstByEmployeeIdAndIsActiveTrue(
                                        employee.getId()
                                )
                                .orElseThrow(
                                        () -> new RuntimeException(
                                                "No market assigned"
                                        )
                                );

                customer.setMarket(
                        assignment.getMarket()
                );
            }
        }
        

        Customer savedCustomer =
                customerRepository.save(customer);
        CreateAddressRequest currentAddress =
                new CreateAddressRequest();

        currentAddress.setCustomerId(savedCustomer.getId());
        currentAddress.setAddressType(AddressType.CURRENT);
        currentAddress.setAddressLine1(request.getCurrentAddress());
        currentAddress.setCity(request.getCity());
        currentAddress.setState(request.getState());
        currentAddress.setPincode(request.getPincode());

        addressService.createAddress(currentAddress);
        
        CreateAddressRequest permanentAddress =
                new CreateAddressRequest();

        permanentAddress.setCustomerId(savedCustomer.getId());
        permanentAddress.setAddressType(AddressType.PERMANENT);
        permanentAddress.setAddressLine1(request.getPermanentAddress());
        permanentAddress.setCity(request.getCity());
        permanentAddress.setState(request.getState());
        permanentAddress.setPincode(request.getPincode());

        addressService.createAddress(permanentAddress);
        
        customerHistoryService.recordHistory(

                savedCustomer,

                CustomerHistoryAction.CUSTOMER_CREATED,

                "Customer Created",

                "Customer profile was created successfully.",

                null,

                savedCustomer.getCustomerCode()
        );
        
        syncLogService.logSync(
                "CUSTOMER",
                savedCustomer.getId().toString()
        );
        
        String employeeNameLog = (employee != null && employee.getFullName() != null) 
                ? employee.getFullName() + " (" + (employee.getRole() != null ? employee.getRole().getRoleName() : "Employee") + ")" 
                : "System/User";
        String custNameStr = (savedCustomer.getFirstName() != null ? savedCustomer.getFirstName() : "") + (savedCustomer.getLastName() != null ? " " + savedCustomer.getLastName() : "");
        auditLogService.log(
                employeeNameLog,
                "Added customer " + custNameStr.trim(),
                "CUSTOMER",
                savedCustomer.getId().toString()
        );

        return customerMapper.toResponse(
                savedCustomer
        );
    }

    @Override
    public CustomerResponse updateCustomer(
            UUID customerId,
            UpdateCustomerRequest request
    ) {
    	

        accessControlService.validateCustomerAccess(
                customerId
        );

        Customer customer =
                customerRepository.findById(customerId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Customer not found"
                                )
                        );
        String oldValue =
                "Name: " +
                customer.getFirstName() +
                " " +
                customer.getLastName() +
                ", Mobile: " +
                customer.getMobileNumber() +
                ", Occupation: " +
                customer.getOccupation();

        customer.setFirstName(
                request.getFirstName()
        );

        customer.setLastName(
                request.getLastName()
        );

        customer.setAlternateMobileNumber(
                request.getAlternateMobileNumber()
        );

        customer.setEmail(
                request.getEmail()
        );

        customer.setDateOfBirth(
                request.getDateOfBirth()
        );

        customer.setGender(
                request.getGender()
        );

        customer.setAadhaarNumber(
                request.getAadhaarNumber()
        );

        customer.setPanNumber(
                request.getPanNumber()
        );

        customer.setOccupation(
                request.getOccupation()
        );

        customer.setMonthlyIncome(
                request.getMonthlyIncome()
        );

        if (request.getMarketId() != null) {
            Market market = marketRepository.findById(request.getMarketId())
                    .orElseThrow(() -> new RuntimeException("Market not found"));
            customer.setMarket(market);
        }

        Customer savedCustomer =
                customerRepository.save(
                        customer
                );
        String newValue =
                "Name: " +
                savedCustomer.getFirstName() +
                " " +
                savedCustomer.getLastName() +
                ", Mobile: " +
                savedCustomer.getMobileNumber() +
                ", Occupation: " +
                savedCustomer.getOccupation();
        customerHistoryService.recordHistory(

                savedCustomer,

                CustomerHistoryAction.CUSTOMER_UPDATED,

                "Customer Details Updated",

                "Customer profile information was updated.",

                oldValue,

                newValue
        );


        auditLogService.log(
                "SYSTEM",
                "UPDATE_CUSTOMER",
                "CUSTOMER",
                savedCustomer.getId().toString()
        );


        syncLogService.logSync(
                "CUSTOMER",
                savedCustomer.getId().toString()
        );


        return customerMapper.toResponse(
                savedCustomer
        );
    }
    
    @Transactional
    @Override
    public DeleteCustomerResponse deleteCustomer(
            UUID customerId,
            UUID deletedBy
    ) {

        Customer customer =
                customerRepository
                        .findByIdAndDeletedFalse(
                                customerId
                        )
                        .orElseThrow(
                        );


        boolean hasBlockingLoan =
                loanRepository
                        .existsByCustomerIdAndLoanStatusIn(
                                customerId,
                                BLOCKING_LOAN_STATUSES
                        );


        if (hasBlockingLoan) {

            throw new IllegalStateException(
                    "Customer cannot be deleted because an active loan relationship exists"
            );

        }


        LocalDateTime deletedAt =
                LocalDateTime.now();


        customer.setDeleted(true);

        customer.setDeletedAt(deletedAt);

        customer.setDeletedBy(deletedBy);


        customerRepository.save(customer);


        return DeleteCustomerResponse.builder()

                .customerId(customer.getId())

                .customerCode(
                        customer.getCustomerCode()
                )

                .customerName(
                        customer.getFirstName()+ customer.getLastName()
                )

                .deletedAt(deletedAt)

                .build();

    }

    @Override
    public CustomerResponse getCustomerById(
            UUID customerId
    ) {
    	accessControlService.validateCustomerAccess(customerId);

        Customer customer =
                customerRepository.findById(customerId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Customer not found"
                        )
                );

        return customerMapper.toResponse(
                customer
        );
    }

    @Override
    public Page<CustomerResponse>
    getAllCustomers(
            int page,
            int size
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String mobileNumber =
                authentication.getName();

        User user =
                userRepository
                        .findByMobileNumber(
                                mobileNumber
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        if(user.getRole()
                .getRoleName()
                .equalsIgnoreCase(
                        "ADMIN"
                )) {

            return customerRepository
                    .findAll(
                            PageRequest.of(
                                    page,
                                    size
                            )
                    )
                    .map(
                            customerMapper::toResponse
                    );
        }

        //----------------------------------------------------------
        // EMPLOYEE
        //----------------------------------------------------------
        EmployeeMarketAssignment assignment =
                assignmentRepository
                        .findFirstByEmployeeIdAndIsActiveTrue(
                                user.getId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "No market assigned"
                                )
                        );

        return customerRepository
                .findByMarketId(
                        assignment.getMarket().getId(),
                        PageRequest.of(
                                page,
                                size
                        )
                )
                .map(
                        customerMapper::toResponse
                );
    }

    @Override
    public Page<CustomerResponse> searchCustomers(
            String keyword,
            int page,
            int size
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String mobileNumber =
                authentication.getName();

        User user =
                userRepository
                        .findByMobileNumber(
                                mobileNumber
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        if(user.getRole()
                .getRoleName()
                .equalsIgnoreCase(
                        "ADMIN"
                )) {

            return customerRepository
                    .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrMobileNumberContaining(
                            keyword,
                            keyword,
                            keyword,
                            PageRequest.of(
                                    page,
                                    size
                            )
                    )
                    .map(
                            customerMapper::toResponse
                    );
        }

        //----------------------------------------------------------
        // EMPLOYEE
        //----------------------------------------------------------
        EmployeeMarketAssignment assignment =
                assignmentRepository
                        .findFirstByEmployeeIdAndIsActiveTrue(
                                user.getId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "No market assigned"
                                )
                        );

        UUID marketId = assignment.getMarket().getId();
        return customerRepository
                .findByMarketIdAndFirstNameContainingIgnoreCaseOrMarketIdAndLastNameContainingIgnoreCaseOrMarketIdAndMobileNumberContaining(
                        marketId,
                        keyword,
                        marketId,
                        keyword,
                        marketId,
                        keyword,
                        PageRequest.of(page, size)
                )
                .map(
                        customerMapper::toResponse
                );
    }

    private String generateCustomerCode() {

        return "CUS-" +
                UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}
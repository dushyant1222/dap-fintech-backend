package com.dapfintech.market.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dapfintech.audit.service.AuditLogService;
import com.dapfintech.customer.repository.CustomerRepository;
import com.dapfintech.loan.enums.LoanStatus;
import com.dapfintech.loan.repository.LoanCollectionRepository;
import com.dapfintech.loan.repository.LoanRepository;
import com.dapfintech.market.dto.request.CreateMarketRequest;
import com.dapfintech.market.dto.request.UpdateMarketRequest;
import com.dapfintech.market.dto.response.AssignmentResponse;
import com.dapfintech.market.dto.response.MarketDashboardResponse;
import com.dapfintech.market.dto.response.MarketDetailsResponse;
import com.dapfintech.market.dto.response.MarketResponse;
import com.dapfintech.market.entity.EmployeeMarketAssignment;
import com.dapfintech.market.entity.Market;
import com.dapfintech.market.enums.MarketStatus;
import com.dapfintech.market.mapper.EmployeeMarketAssignmentMapper;
import com.dapfintech.market.mapper.MarketMapper;
import com.dapfintech.market.repository.EmployeeMarketAssignmentRepository;
import com.dapfintech.market.repository.MarketRepository;
import com.dapfintech.market.service.MarketService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketServiceImpl
        implements MarketService {

    private final MarketRepository marketRepository;

    private final MarketMapper marketMapper;

    private final EmployeeMarketAssignmentRepository
            assignmentRepository;

    private final AuditLogService auditLogService;
    private final CustomerRepository customerRepository;

    private final LoanRepository loanRepository;

    private final LoanCollectionRepository loanCollectionRepository;
    private final EmployeeMarketAssignmentMapper assignmentMapper;
    
    
    @Override
    public MarketDashboardResponse
    getDashboard(UUID marketId) {

        Market market =
                marketRepository.findById(marketId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Market not found"
                        )
                );

        Long totalCustomers =
                customerRepository.countByMarketId(
                        marketId
                );

        Long activeLoans =
                loanRepository
                        .countByCustomerMarketIdAndLoanStatus(
                                marketId,
                                LoanStatus.ACTIVE
                        );

        Long totalCollections =
                loanCollectionRepository
                        .countByLoanCustomerMarketId(
                                marketId
                        );

        BigDecimal collectionAmount =
                loanCollectionRepository
                        .getTotalCollectionAmount(
                                marketId
                        );

        List<AssignmentResponse> employees =
                assignmentRepository
                        .findByMarketIdAndIsActiveTrue(
                                marketId
                        )
                        .stream()
                        .map(
                                assignmentMapper::toResponse
                        )
                        .toList();

        return MarketDashboardResponse
                .builder()

                .marketId(
                        market.getId()
                )

                .marketCode(
                        market.getMarketCode()
                )

                .marketName(
                        market.getMarketName()
                )

                .city(
                        market.getCity()
                )

                .state(
                        market.getState()
                )

                .description(
                        market.getDescription()
                )

                .status(
                        market.getStatus().name()
                )

                .totalCustomers(
                        totalCustomers
                )

                .activeLoans(
                        activeLoans
                )

                .totalCollections(
                        totalCollections
                )

                .totalCollectionAmount(
                        collectionAmount
                )

                .employees(
                        employees
                )

                .build();
    }

    // =====================================================
    // CREATE MARKET
    // =====================================================

    @Override
    @Transactional
    public MarketResponse createMarket(
            CreateMarketRequest request
    ) {

        validateCreateRequest(request);

        String marketName =
                request.getMarketName().trim();

        String city =
                request.getCity().trim();

        if (
                marketRepository
                        .existsByMarketNameIgnoreCaseAndCityIgnoreCase(
                                marketName,
                                city
                        )
        ) {

            throw new RuntimeException(
                    "A market with this name already exists in this city"
            );
        }

        Market market =
                Market.builder()
                        .marketCode(
                                generateUniqueMarketCode()
                        )
                        .marketName(
                                marketName
                        )
                        .city(
                                city
                        )
                        .state(
                                cleanNullable(
                                        request.getState()
                                )
                        )
                        .description(
                                cleanNullable(
                                        request.getDescription()
                                )
                        )
                        .status(
                                MarketStatus.ACTIVE
                        )
                        .build();

        market =
                marketRepository.save(
                        market
                );

        auditLogService.log(
                "SYSTEM",
                "CREATE_MARKET",
                "MARKET",
                market.getId().toString()
        );

        return marketMapper.toResponse(
                market
        );
    }

    // =====================================================
    // UPDATE MARKET
    // =====================================================

    @Override
    @Transactional
    public MarketResponse updateMarket(
            UUID marketId,
            UpdateMarketRequest request
    ) {

        Market market =
                getMarketEntity(marketId);

        if (request == null) {
            throw new RuntimeException(
                    "Market update request is required"
            );
        }

        if (
                request.getMarketName() != null &&
                !request.getMarketName()
                        .trim()
                        .isEmpty()
        ) {

            market.setMarketName(
                    request.getMarketName().trim()
            );
        }

        if (
                request.getCity() != null &&
                !request.getCity()
                        .trim()
                        .isEmpty()
        ) {

            market.setCity(
                    request.getCity().trim()
            );
        }

        if (request.getState() != null) {

            market.setState(
                    cleanNullable(
                            request.getState()
                    )
            );
        }

        if (request.getDescription() != null) {

            market.setDescription(
                    cleanNullable(
                            request.getDescription()
                    )
            );
        }

        market =
                marketRepository.save(
                        market
                );

        auditLogService.log(
                "SYSTEM",
                "UPDATE_MARKET",
                "MARKET",
                market.getId().toString()
        );

        return marketMapper.toResponse(
                market
        );
    }

    // =====================================================
    // GET MARKET BY ID
    // =====================================================

    @Override
    public MarketDetailsResponse getMarketDetails(UUID marketId) {

        Market market =
                marketRepository
                        .findById(marketId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Market not found"
                                )
                        );

        List<EmployeeMarketAssignment> assignments =
                assignmentRepository
                        .findByMarketIdAndIsActiveTrue(marketId);

        return MarketDetailsResponse.builder()

                .id(market.getId())

                .marketCode(market.getMarketCode())

                .marketName(market.getMarketName())

                .city(market.getCity())

                .state(market.getState())

                .description(market.getDescription())

                .status(market.getStatus())

                .employeeCount(assignments.size())

                .assignedEmployees(

                        assignments.stream()

                                .map(assignmentMapper::toResponse)

                                .toList()

                )

                .build();
    }

    // =====================================================
    // GET ALL MARKETS
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<MarketResponse>
    getAllMarkets() {

        return marketRepository
                .findAllByOrderByMarketNameAsc()
                .stream()
                .map(
                        marketMapper::toResponse
                )
                .toList();
    }

    // =====================================================
    // GET ACTIVE MARKETS
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<MarketResponse>
    getActiveMarkets() {

        return marketRepository
                .findByStatusOrderByMarketNameAsc(
                        MarketStatus.ACTIVE
                )
                .stream()
                .map(
                        marketMapper::toResponse
                )
                .toList();
    }

    // =====================================================
    // ACTIVATE MARKET
    // =====================================================

    @Override
    @Transactional
    public MarketResponse activateMarket(
            UUID marketId
    ) {

        Market market =
                getMarketEntity(marketId);

        if (
                market.getStatus() ==
                MarketStatus.ACTIVE
        ) {

            return marketMapper.toResponse(
                    market
            );
        }

        market.setStatus(
                MarketStatus.ACTIVE
        );

        market =
                marketRepository.save(
                        market
                );

        auditLogService.log(
                "SYSTEM",
                "ACTIVATE_MARKET",
                "MARKET",
                market.getId().toString()
        );

        return marketMapper.toResponse(
                market
        );
    }

    // =====================================================
    // DEACTIVATE MARKET
    // =====================================================

    @Override
    @Transactional
    public MarketResponse deactivateMarket(
            UUID marketId
    ) {

        Market market =
                getMarketEntity(marketId);

        if (
                market.getStatus() ==
                MarketStatus.INACTIVE
        ) {

            return marketMapper.toResponse(
                    market
            );
        }

        market.setStatus(
                MarketStatus.INACTIVE
        );

        market =
                marketRepository.save(
                        market
                );

        auditLogService.log(
                "SYSTEM",
                "DEACTIVATE_MARKET",
                "MARKET",
                market.getId().toString()
        );

        return marketMapper.toResponse(
                market
        );
    }

    // =====================================================
    // DELETE MARKET
    // =====================================================

    @Override
    @Transactional
    public void deleteMarket(
            UUID marketId
    ) {

        Market market =
                getMarketEntity(marketId);

        boolean hasActiveEmployees =
                assignmentRepository
                        .existsByMarketIdAndIsActiveTrue(
                                marketId
                        );

        if (hasActiveEmployees) {

            throw new RuntimeException(
                    "Cannot delete market because employees are currently assigned to it"
            );
        }

        boolean hasCustomers = customerRepository.countByMarketId(marketId) > 0;
        if (hasCustomers) {
            throw new RuntimeException(
                    "Cannot delete market because customers are assigned to it"
            );
        }

        auditLogService.log(
                "SYSTEM",
                "DELETE_MARKET",
                "MARKET",
                market.getId().toString()
        );

        marketRepository.delete(
                market
        );
    }

    // =====================================================
    // GET MARKET ENTITY
    // =====================================================

    private Market getMarketEntity(
            UUID marketId
    ) {

        if (marketId == null) {

            throw new RuntimeException(
                    "Market ID is required"
            );
        }

        return marketRepository
                .findById(marketId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Market not found"
                        )
                );
    }

    // =====================================================
    // VALIDATE CREATE REQUEST
    // =====================================================

    private void validateCreateRequest(
            CreateMarketRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Market request is required"
            );
        }

        if (
                request.getMarketName() == null ||
                request.getMarketName()
                        .trim()
                        .isEmpty()
        ) {

            throw new RuntimeException(
                    "Market name is required"
            );
        }

        if (
                request.getCity() == null ||
                request.getCity()
                        .trim()
                        .isEmpty()
        ) {

            throw new RuntimeException(
                    "City is required"
            );
        }
    }

    // =====================================================
    // GENERATE UNIQUE MARKET CODE
    // =====================================================

    private String generateUniqueMarketCode() {

        String marketCode;

        do {

            marketCode =
                    "MKT-" +
                    UUID.randomUUID()
                            .toString()
                            .substring(0, 6)
                            .toUpperCase();

        } while (
                marketRepository
                        .existsByMarketCode(
                                marketCode
                        )
        );

        return marketCode;
    }

    // =====================================================
    // CLEAN NULLABLE STRING
    // =====================================================

    private String cleanNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String cleaned =
                value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }
}
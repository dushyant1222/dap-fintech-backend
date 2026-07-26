package com.dapfintech.market.entity;
import java.util.UUID;

import com.dapfintech.common.base.BaseEntity;
import com.dapfintech.market.enums.MarketStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name="markets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Market extends BaseEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@Column(name="market_code")
	private String marketCode;
	
	@Column(name="market_name")
	private String marketName;
	
	private String city;
	
	private String state;
	
	private String description;
	
	@Enumerated(EnumType.STRING)
	private MarketStatus status;

}

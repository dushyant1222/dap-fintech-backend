package com.dapfintech.sync.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.dapfintech.sync.enums.SyncStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="sync_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncLog {
	
	 	@Id
	    @GeneratedValue(strategy = GenerationType.UUID)
	    private UUID id;
	 	
	 	@Column(name="entity_type")
	 	private String entityType;
	 	
	 	@Column(name="entity_id")
	 	private String entityId;
	 	
	 	@Enumerated(EnumType.STRING)
	 	@Column(name="sync_status")
	 	private SyncStatus syncStatus;
	 	
	 	@Column(name="sync_time")
	 	private LocalDateTime syncTime;

}

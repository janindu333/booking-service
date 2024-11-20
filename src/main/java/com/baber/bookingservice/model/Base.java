package com.baber.bookingservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.util.Date;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Base implements Serializable {
    @Transient
    private static final long serialVersionUID = -1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    @CreatedBy
    String createdBy;

    @LastModifiedBy
    String modifiedBy;
    @CreatedDate
    Date createdOn;
    @LastModifiedDate
    Date modifiedOn;
    @Version
    Long version;
    @JsonIgnore
    private Integer deleted = 0;
    public void setDeleted() {
        this.deleted = 1;
    }
}

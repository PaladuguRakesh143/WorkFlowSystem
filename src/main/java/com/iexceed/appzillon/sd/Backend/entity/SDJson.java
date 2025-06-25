package com.iexceed.appzillon.sd.Backend.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "TB_SDUI_SCREEN_DATA")
@Data
public class SDJson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Assuming there's an ID, add it if not present

    @Column(name = "MODULE_NAME", nullable = false)
    private String moduleName;

    @Column(name = "SCREEN_NAME", nullable = false)
    private String screenName;

    @Column(name = "JSON", columnDefinition = "JSON")
    private String json;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATE_TS")
    private Date createTs;

    @Column(name = "UPDATE_TS")
    private Date updateTs;

    public String getJson() {
        return this.json;
    }
}

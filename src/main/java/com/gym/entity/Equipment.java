package com.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("equipment")
public class Equipment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String brand;
    private String model;
    private String location;
    private LocalDate purchaseDate;
    private BigDecimal price;
    private Integer status;
    private LocalDate lastMaintenance;
    private LocalDate nextMaintenance;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDate getLastMaintenance() { return lastMaintenance; }
    public void setLastMaintenance(LocalDate lastMaintenance) { this.lastMaintenance = lastMaintenance; }
    public LocalDate getNextMaintenance() { return nextMaintenance; }
    public void setNextMaintenance(LocalDate nextMaintenance) { this.nextMaintenance = nextMaintenance; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}

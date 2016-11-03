package com.ly.citypickerview.model;

public class County {

    private String areaId;
    private String areaName;

    public County() {
        super();
    }

    public County(String areaName, String areaId) {
        super();
        this.areaName = areaName;
        this.areaId = areaId;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

}

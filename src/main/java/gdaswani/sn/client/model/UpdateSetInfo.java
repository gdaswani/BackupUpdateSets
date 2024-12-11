package gdaswani.sn.client.model;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class UpdateSetInfo {

    private String sysId;
    private String name;

    @JsonProperty("sys_id")
    public String getSysId() {
	return sysId;
    }

    public void setSysId(String sysId) {
	this.sysId = sysId;
    }

    @JsonProperty("name")
    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String toString() {
	return new ReflectionToStringBuilder(this).appendSuper(super.toString()).toString();
    }

}

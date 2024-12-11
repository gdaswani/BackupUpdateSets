package gdaswani.sn.client.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class UpdateSetListResult {

    private List<UpdateSetInfo> result;

    public UpdateSetListResult() {
	super();
	result = new ArrayList<>();
    }

    @JsonProperty("result")
    public List<UpdateSetInfo> getResult() {
	return result;
    }

    public void setValues(List<UpdateSetInfo> result) {
	this.result = result;
    }

    public String toString() {
	return new ReflectionToStringBuilder(this).appendSuper(super.toString()).toString();
    }

}

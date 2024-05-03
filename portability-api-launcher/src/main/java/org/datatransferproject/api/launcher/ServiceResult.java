package org.datatransferproject.api.launcher;
public class ServiceResult{
    private java.lang.String service;

    public java.lang.String getService(){
        return service;
    }

    public void setService(java.lang.String service){
        this.service=service;
    }

    private boolean success;

    public boolean getSuccess(){
        return success;
    }

    public void setSuccess(boolean success){
        this.success=success;
    }

    private java.time.Duration duration;

    public java.time.Duration getDuration(){
        return duration;
    }

    public void setDuration(java.time.Duration duration){
        this.duration=duration;
    }

    public ServiceResult(java.lang.String service,boolean success,java.time.Duration duration){
        this.service=service;
        this.success=success;
        this.duration=duration;
    }
}


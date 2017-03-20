package org.dataportabilityproject.serviceProviders.rememberTheMilk;


enum RememberTheMilkMethods {
    CHECK_TOKEN("rtm.auth.checkToken"),
    GET_FROB("rtm.auth.getFrob"),
    LISTS_GET_LIST("rtm.lists.getList"),
    LISTS_ADD("rtm.lists.add"),
    GET_TOKEN("rtm.auth.getToken"),
    TASKS_GET_LIST("rtm.tasks.getList"),
    TASK_ADD("rtm.tasks.add"),
    TIMELINES_CREATE("rtm.timelines.create"),;

    private static final String BASE_URL = "https://api.rememberthemilk.com/services/rest/";

    RememberTheMilkMethods(String methodName) {
        this.methodName = methodName;
    }

    private final String methodName;

    String getMethodName() {
        return methodName;
    }

    String getUrl() {
        return BASE_URL + "?method=" + getMethodName();
    }

}

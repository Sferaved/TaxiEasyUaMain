package com.taxi.easy.ua.ui.finish;

import com.google.gson.annotations.SerializedName;

public class OrderResponse {
    @SerializedName("change_cost_allowed")
    private boolean changeCostAllowed;

    @SerializedName("change_cost_not_allowed_reason")
    private int changeCostNotAllowedReason;

    @SerializedName("dispatching_order_uid")
    private String dispatchingOrderUid;

    @SerializedName("order_cost")
    private String orderCost;

    @SerializedName("add_cost")
    private String addCost;

    @SerializedName("currency")
    private String currency;

    @SerializedName("order_car_info")
    private String orderCarInfo;

    @SerializedName("driver_phone")
    private String driverPhone;

    @SerializedName("drivercar_position")
    private String driverCarPosition;

    @SerializedName("required_time")
    private String requiredTime;

    @SerializedName("close_reason")
    private int closeReason;

    @SerializedName("cancel_reason_comment")
    private String cancelReasonComment;

    @SerializedName("order_is_archive")
    private boolean orderIsArchive;

    @SerializedName("driver_execution_status")
    private int driverExecutionStatus;

    @SerializedName("create_date_time")
    private String createDateTime;

    @SerializedName("find_car_timeout")
    private int findCarTimeout;

    @SerializedName("find_car_delay")
    private int findCarDelay;

    @SerializedName("execution_status")
    private String executionStatus;

    @SerializedName("cancellation_reason")
    private int cancellationReason;

    @SerializedName("crew_average_rating")
    private String crewAverageRating;

    @SerializedName("rating")
    private String rating;

    @SerializedName("rating_comment")
    private String ratingComment;

    @SerializedName("corporate_account_id")
    private int corporateAccountId;

    @SerializedName("push_type")
    private int pushType;

    @SerializedName("time_to_start_point")
    private String timeToStartPoint;

    @SerializedName("action")
    private String action;

    public String getUid() {
        return uid;
    }

    @SerializedName("uid")
    private String uid;

    // Getters
    public boolean isChangeCostAllowed() {
        return changeCostAllowed;
    }

    public int getChangeCostNotAllowedReason() {
        return changeCostNotAllowedReason;
    }

    public String getDispatchingOrderUid() {
        return dispatchingOrderUid;
    }

    public String getOrderCost() {
        return orderCost;
    }

    public String getAddCost() {
        return addCost;
    }

    public String getCurrency() {
        return currency;
    }

    public String getOrderCarInfo() {
        return orderCarInfo;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public String getDriverCarPosition() {
        return driverCarPosition;
    }

    public String getRequiredTime() {
        return requiredTime;
    }

    public String getTimeToStartPoint() {
        return timeToStartPoint;
    }

    public int getCloseReason() {
        return closeReason;
    }

    public String getCancelReasonComment() {
        return cancelReasonComment;
    }

    public boolean isOrderIsArchive() {
        return orderIsArchive;
    }

    public int getDriverExecutionStatus() {
        return driverExecutionStatus;
    }

    public String getCreateDateTime() {
        return createDateTime;
    }

    public int getFindCarTimeout() {
        return findCarTimeout;
    }

    public int getFindCarDelay() {
        return findCarDelay;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public int getCancellationReason() {
        return cancellationReason;
    }

    public String getCrewAverageRating() {
        return crewAverageRating;
    }

    public String getRating() {
        return rating;
    }

    public String getRatingComment() {
        return ratingComment;
    }

    public int getCorporateAccountId() {
        return corporateAccountId;
    }

    public int getPushType() {
        return pushType;
    }

    // Setters
    public void setChangeCostAllowed(boolean changeCostAllowed) {
        this.changeCostAllowed = changeCostAllowed;
    }

    public void setChangeCostNotAllowedReason(int changeCostNotAllowedReason) {
        this.changeCostNotAllowedReason = changeCostNotAllowedReason;
    }

    public void setDispatchingOrderUid(String dispatchingOrderUid) {
        this.dispatchingOrderUid = dispatchingOrderUid;
    }

    public void setOrderCost(String orderCost) {
        this.orderCost = orderCost;
    }

    public void setAddCost(String addCost) {
        this.addCost = addCost;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setOrderCarInfo(String orderCarInfo) {
        this.orderCarInfo = orderCarInfo;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public void setDriverCarPosition(String driverCarPosition) {
        this.driverCarPosition = driverCarPosition;
    }

    public void setRequiredTime(String requiredTime) {
        this.requiredTime = requiredTime;
    }

    public void setCloseReason(int closeReason) {
        this.closeReason = closeReason;
    }

    public void setCancelReasonComment(String cancelReasonComment) {
        this.cancelReasonComment = cancelReasonComment;
    }

    public void setOrderIsArchive(boolean orderIsArchive) {
        this.orderIsArchive = orderIsArchive;
    }


    public void setDriverExecutionStatus(int driverExecutionStatus) {
        this.driverExecutionStatus = driverExecutionStatus;
    }

    public void setCreateDateTime(String createDateTime) {
        this.createDateTime = createDateTime;
    }

    public void setFindCarTimeout(int findCarTimeout) {
        this.findCarTimeout = findCarTimeout;
    }

    public void setFindCarDelay(int findCarDelay) {
        this.findCarDelay = findCarDelay;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public void setCancellationReason(int cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public void setCrewAverageRating(String crewAverageRating) {
        this.crewAverageRating = crewAverageRating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public void setRatingComment(String ratingComment) {
        this.ratingComment = ratingComment;
    }

    public void setCorporateAccountId(int corporateAccountId) {
        this.corporateAccountId = corporateAccountId;
    }

    public void setPushType(int pushType) {
        this.pushType = pushType;
    }

    public String getAction() {
        return action;
    }
}


package com.finsplit.app.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * OOP — Encapsulation: all fields private with public getters/setters.
 * Stored in Firestore at /users/{uid}/weeklyReports/{reportId}.
 */
public class WeeklyReport {

    @DocumentId
    private String reportId;
    private String userId;
    private double totalSpentPKR;
    private String topCategory;      // Category.name() string
    private double totalOwed;        // amount others owe you
    private double totalOwedToUser;  // amount you owe others
    private String weekRange;        // e.g. "Jun 9 – Jun 15"

    @ServerTimestamp
    private Date generatedAt;

    public WeeklyReport() {}

    public WeeklyReport(String userId, double totalSpentPKR, String topCategory,
                        double totalOwed, double totalOwedToUser, String weekRange) {
        this.userId = userId;
        this.totalSpentPKR = totalSpentPKR;
        this.topCategory = topCategory;
        this.totalOwed = totalOwed;
        this.totalOwedToUser = totalOwedToUser;
        this.weekRange = weekRange;
    }

    public String getReportId()        { return reportId; }
    public String getUserId()          { return userId; }
    public double getTotalSpentPKR()   { return totalSpentPKR; }
    public String getTopCategory()     { return topCategory; }
    public double getTotalOwed()       { return totalOwed; }
    public double getTotalOwedToUser() { return totalOwedToUser; }
    public String getWeekRange()       { return weekRange; }
    public Date   getGeneratedAt()     { return generatedAt; }

    public void setReportId(String reportId)               { this.reportId = reportId; }
    public void setUserId(String userId)                   { this.userId = userId; }
    public void setTotalSpentPKR(double totalSpentPKR)     { this.totalSpentPKR = totalSpentPKR; }
    public void setTopCategory(String topCategory)         { this.topCategory = topCategory; }
    public void setTotalOwed(double totalOwed)             { this.totalOwed = totalOwed; }
    public void setTotalOwedToUser(double v)               { this.totalOwedToUser = v; }
    public void setWeekRange(String weekRange)             { this.weekRange = weekRange; }
    public void setGeneratedAt(Date generatedAt)           { this.generatedAt = generatedAt; }
}

# 📊 Analytics & Reporting System

## Overview

The iScan app now includes a comprehensive analytics and reporting system that automatically tracks user activity, app usage metrics, errors, and performance data. All reports are sent to Firebase Firestore where you can view them.

## 🎯 What Gets Tracked

### 1. **User Activity**
- Screen views (which screens users visit)
- Time spent on each screen
- Navigation patterns
- Session duration

### 2. **Feature Usage**
- Exam creation/deletion
- Scan operations (success/failure)
- Answer key settings
- Export operations (CSV, PDF)
- Template generation
- Backup/restore operations

### 3. **Performance Metrics**
- Scan processing time
- App launch time
- Feature execution duration
- Session metrics (screen views, actions per session)

### 4. **Error Tracking**
- Scan errors
- Export failures
- Database errors
- Authentication issues
- Complete stack traces for debugging

### 5. **Usage Metrics**
- Daily active users
- Event frequency (how often features are used)
- Most popular features
- User retention patterns

## 📂 Firebase Collections

All data is stored in Firebase Firestore in these collections:

### **analytics_events**
Individual events with detailed metadata
```
- event_name: string (e.g., "exam_created", "scan_completed")
- category: string (e.g., "user_action", "feature_usage")
- timestamp: number (milliseconds)
- datetime: string (human-readable)
- user_id: string (Firebase Auth UID or "anonymous")
- properties: map (event-specific data)
```

### **daily_metrics**
Aggregated daily statistics per user
```
- user_id: string
- date: string (YYYY-MM-DD)
- total_events: number
- events: map (event_name → count)
- categories: map (category_name → count)
- last_updated: number
```

### **developer_alerts**
Critical errors and issues requiring attention
```
- alert_type: string (e.g., "scan_error", "crash")
- message: string
- user_id: string
- timestamp: number
- datetime: string
- metadata: map (error details)
- severity: string ("critical", "high", "medium", "low")
- read: boolean (false by default)
```

### **usage_reports**
Periodic usage summaries
```
- report_type: string ("daily", "weekly", "session")
- user_id: string
- date: string
- generated_at: number
- metrics: map (aggregated data)
- read: boolean
```

### **developer_reports**
Weekly/monthly summary reports
```
- report_type: string ("weekly_summary", "monthly_summary")
- data: map (comprehensive statistics)
- generated_at: number
- read: boolean
```

## 🔍 How to View Reports

### Option 1: Firebase Console (Easiest)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **iScan** (or your project name)
3. Navigate to **Firestore Database** in the left sidebar
4. Browse the collections listed above

#### Quick Queries:

**View Recent Errors:**
- Collection: `developer_alerts`
- Order by: `timestamp` (descending)
- Limit: 50

**View Today's Activity:**
- Collection: `daily_metrics`
- Where: `date` == "2026-09-04" (today's date)

**View Most Active Users:**
- Collection: `daily_metrics`
- Order by: `total_events` (descending)

**View Unread Alerts:**
- Collection: `developer_alerts`
- Where: `read` == false
- Order by: `severity` then `timestamp`

### Option 2: Firebase CLI

```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login
firebase login

# Query examples
firebase firestore:query developer_alerts --limit 10 --orderBy timestamp --desc

firebase firestore:query daily_metrics --where date==2026-09-04
```

### Option 3: Custom Admin Dashboard (Recommended for Production)

Create a simple web dashboard to view reports:

```javascript
// Example: Fetch recent alerts
const alerts = await db.collection('developer_alerts')
  .where('read', '==', false)
  .orderBy('severity')
  .orderBy('timestamp', 'desc')
  .limit(50)
  .get();

alerts.forEach(doc => {
  console.log(doc.data());
});
```

## 📈 Key Metrics to Monitor

### Daily Health Check
1. **Error Rate**: Check `developer_alerts` for new critical errors
2. **Active Users**: Count unique users in `daily_metrics`
3. **Feature Usage**: Which features are used most?
4. **Scan Success Rate**: Scan completion vs. failures

### Weekly Review
1. **User Retention**: How many users return?
2. **Top Features**: What's being used most?
3. **Error Trends**: Are errors increasing?
4. **Performance**: Average scan/processing times

### Monthly Analysis
1. **User Growth**: New users vs. churned users
2. **Feature Adoption**: Which features are underutilized?
3. **Crash Rate**: Overall app stability
4. **Engagement**: Sessions per user, time in app

## 🔔 Alert Severity Levels

- **CRITICAL**: Crashes, data loss, auth failures → Immediate action required
- **HIGH**: Feature failures, repeated errors → Fix within 24 hours
- **MEDIUM**: Warnings, degraded performance → Monitor and fix soon
- **LOW**: Info, minor issues → Track for patterns

## 🛡️ Privacy & Compliance

### What's Collected:
- ✅ User ID (Firebase Auth UID, anonymized)
- ✅ Feature usage (what buttons clicked)
- ✅ Performance metrics (how long operations take)
- ✅ Error logs (what went wrong)
- ✅ Screen navigation (which screens viewed)

### What's NOT Collected:
- ❌ Personal student data (names, IDs, scores)
- ❌ Exam content (questions, answers)
- ❌ Device location
- ❌ Contacts or other device data
- ❌ Sensitive user information

### Privacy Policy Disclosure:
Users are informed in the Privacy Policy that:
- Anonymous usage data is collected to improve the app
- No personal student data is transmitted
- All data stays encrypted and secure
- Users can contact you to request data deletion

## 📊 Sample Queries

### Find users having scan issues:
```javascript
db.collection('developer_alerts')
  .where('alert_type', '==', 'scan_error')
  .where('timestamp', '>', Date.now() - 7*24*60*60*1000) // Last 7 days
  .get()
```

### Get most popular features today:
```javascript
const today = '2026-09-04';
const metrics = await db.collection('daily_metrics')
  .where('date', '==', today)
  .get();

const featureCounts = {};
metrics.forEach(doc => {
  const events = doc.data().events || {};
  Object.entries(events).forEach(([event, count]) => {
    featureCounts[event] = (featureCounts[event] || 0) + count;
  });
});

console.log('Top Features:', Object.entries(featureCounts)
  .sort((a,b) => b[1] - a[1])
  .slice(0, 10)
);
```

### Find users with high error rates:
```javascript
const alerts = await db.collection('developer_alerts')
  .where('timestamp', '>', Date.now() - 24*60*60*1000) // Last 24 hours
  .get();

const errorsByUser = {};
alerts.forEach(doc => {
  const userId = doc.data().user_id;
  errorsByUser[userId] = (errorsByUser[userId] || 0) + 1;
});

console.log('Users with errors:', errorsByUser);
```

## 🔧 Testing the System

To verify analytics are working:

1. **Launch the app** → Check `analytics_events` for "app_launch" event
2. **Navigate screens** → Check for screen view events
3. **Create an exam** → Check for "exam_created" event
4. **Scan a sheet** → Check for "scan_completed" or "scan_failed" event
5. **Force an error** → Check `developer_alerts` for the error

## 🚀 Next Steps

### Immediate:
1. ✅ Analytics system implemented
2. ✅ Session tracking active
3. ✅ Error reporting configured

### Recommended:
1. Set up Firebase Cloud Functions to send email notifications for critical alerts
2. Create a simple admin dashboard (React/Next.js) to view reports
3. Set up automated weekly summary emails
4. Configure Firebase Performance Monitoring for additional insights

### Advanced:
1. Implement A/B testing for features
2. Add custom cohort analysis
3. Set up retention/churn predictions
4. Create custom dashboards with charts/graphs

## 📧 Email Notifications (Optional)

To get email alerts for critical issues:

1. Set up Firebase Cloud Functions
2. Add this function:

```javascript
exports.sendCriticalAlert = functions.firestore
  .document('developer_alerts/{alertId}')
  .onCreate(async (snap, context) => {
    const alert = snap.data();
    
    if (alert.severity === 'critical') {
      // Send email using SendGrid, AWS SES, or Firebase Email Extension
      await sendEmail({
        to: 'your-email@example.com',
        subject: `🚨 Critical Alert: ${alert.alert_type}`,
        body: `
          Alert Type: ${alert.alert_type}
          Message: ${alert.message}
          User: ${alert.user_id}
          Time: ${alert.datetime}
          
          View in console: https://console.firebase.google.com/...
        `
      });
    }
  });
```

## 🎯 Success Metrics

Track these KPIs:
- **Daily Active Users (DAU)**
- **Scan Success Rate** (completed / attempted)
- **Feature Adoption Rate** (users who tried each feature)
- **Error Rate** (errors per 1000 events)
- **Session Duration** (how long users stay in app)
- **Retention Rate** (users who return after 1, 7, 30 days)

---

## Support

Questions? Issues with analytics?
- Check Firebase Console logs
- Review `developer_alerts` collection for system errors
- Contact: [Your Email]

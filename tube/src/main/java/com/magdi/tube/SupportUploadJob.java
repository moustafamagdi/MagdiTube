package com.magdi.tube;
public final class SupportUploadJob extends android.app.job.JobService {
 public boolean onStartJob(android.app.job.JobParameters p){SupportReporter.automatic(()->new android.os.Handler(android.os.Looper.getMainLooper()).post(()->jobFinished(p,false)));return true;}
 public boolean onStopJob(android.app.job.JobParameters p){return false;}
}

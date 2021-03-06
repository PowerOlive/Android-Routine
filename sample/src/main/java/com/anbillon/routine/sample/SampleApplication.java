package com.anbillon.routine.sample;

import android.app.Application;
import com.anbillon.routine.Routine;
import com.anbillon.routine.adapter.rxjava.RxJavaAdapterFactory;
import com.anbillon.routine.logging.RoutineLoggingInterceptor;
import com.anbillon.routine.sample.interceptor.IdentityInterceptor;
import com.anbillon.routine.sample.interceptor.NotFoundInterceptor;
import com.anbillon.routine.sample.interceptor.RoutineAuthInterceptor;
import com.anbillon.routine.support.SupportFragmentResolverFactory;

/**
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class SampleApplication extends Application {
  private Navigator navigator;

  @Override public void onCreate() {
    super.onCreate();

    Routine routine =
        new Routine.Builder().addResolverFactory(SupportFragmentResolverFactory.create())
            .addAdapterFactory(RxJavaAdapterFactory.create())
            .addFilter(new SchemeFilter())
            .addInterceptor(new RoutineAuthInterceptor())
            .addInterceptor(new IdentityInterceptor())
            .addInterceptor(new NotFoundInterceptor())
            .addInterceptor(
                new RoutineLoggingInterceptor().setLevel(RoutineLoggingInterceptor.Level.ALL))
            .build();

    navigator = routine.create(Navigator.class);
  }

  public Navigator navigator() {
    return navigator;
  }
}

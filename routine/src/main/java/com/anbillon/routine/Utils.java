/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anbillon.routine;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Parcelable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Utils {

  private Utils() {
    /* no instance */
  }

  /**
   * To check if the given {@code object} is null.
   *
   * @param object object to check
   * @param message message to throw
   * @param <T> type of object
   * @return true if object is null, otherwise return false
   */
  static <T> T checkNotNull(T object, String message) {
    if (object == null) {
      throw new NullPointerException(message);
    }
    return object;
  }

  /**
   * Returns an immutable copy of {@code list}.
   */
  static <T> List<T> immutableList(List<T> list) {
    return Collections.unmodifiableList(new ArrayList<>(list));
  }

  /**
   * Validate the interfaces of router.
   *
   * @param router router interface
   * @param <T> type of router
   */
  static <T> void validateRouterInterface(Class<T> router) {
    if (!router.isInterface()) {
      throw new IllegalArgumentException("API declarations must be interfaces.");
    }

    /*
     * Prevent API interfaces origin extending other interfaces. This not only avoids a bug in
     * Android (http://b.android.com/58753) but it forces composition of API declarations which is
     * the recommended pattern.
     */
    if (router.getInterfaces().length > 0) {
      throw new IllegalArgumentException("API interfaces must not extend other interfaces.");
    }
  }

  /**
   * To check if there's unresolvable {@link Type}.
   *
   * @param type {@link Type} to check
   * @return true if has, otherwise return false
   */
  static boolean hasUnresolvableType(Type type) {
    if (type instanceof Class<?>) {
      return false;
    }
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
        if (hasUnresolvableType(typeArgument)) {
          return true;
        }
      }
      return false;
    }
    if (type instanceof GenericArrayType) {
      return hasUnresolvableType(((GenericArrayType) type).getGenericComponentType());
    }
    if (type instanceof TypeVariable) {
      return true;
    }
    if (type instanceof WildcardType) {
      return true;
    }
    String className = type == null ? "null" : type.getClass().getName();
    throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
        + "GenericArrayType, but <"
        + type
        + "> is of type "
        + className);
  }

  static boolean isSerializableType(Class<?> type) {
    return type == Serializable.class
        || type == Serializable[].class
        || type == Parcelable.class
        || type == Parcelable[].class;
  }

  static Class<?> getExtraRawType(Type type) {
    Class<?> rawType = getRawType(type);
    if (!(type instanceof Class<?>)) {
      return rawType;
    }

    Class<?>[] interfaces = ((Class<?>) type).getInterfaces();
    if (interfaces.length != 0) {
      for (Class<?> interfaceClazz : interfaces) {
        if (isSerializableType(interfaceClazz)) {
          return interfaceClazz;
        }
      }
    }

    return rawType;
  }

  static Class<?> getRawType(Type type) {
    if (type == null) throw new NullPointerException("type == null");

    if (type instanceof Class<?>) {
      /* type is a normal class */
      return (Class<?>) type;
    }

    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;

      /*
       * I'm not exactly sure why getRawType() returns Type instead of Class. Neal isn't either but
       * suspects some pathological case related to nested classes exists.
       */
      Type rawType = parameterizedType.getRawType();
      if (!(rawType instanceof Class)) throw new IllegalArgumentException();
      return (Class<?>) rawType;
    }

    if (type instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) type).getGenericComponentType();
      return Array.newInstance(getRawType(componentType), 0).getClass();
    }

    if (type instanceof TypeVariable) {
      /*
       * We could use the variable's bounds, but that won't work if there are multiple. Having a raw
       * type that's more general than necessary is okay.
       */
      return Object.class;
    }

    if (type instanceof WildcardType) {
      return getRawType(((WildcardType) type).getUpperBounds()[0]);
    }

    throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
        + "GenericArrayType, but <"
        + type
        + "> is of type "
        + type.getClass().getName());
  }

  static Type getParameterUpperBound(int index, ParameterizedType type) {
    Type[] types = type.getActualTypeArguments();
    if (index < 0 || index >= types.length) {
      throw new IllegalArgumentException(
          "Index " + index + " not in range [0," + types.length + ") for " + type);
    }
    Type paramType = types[index];
    if (paramType instanceof WildcardType) {
      return ((WildcardType) paramType).getUpperBounds()[0];
    }
    return paramType;
  }

  /**
   * Resolve the {@link Intent} into an {@link ActivityInfo} describing the activity that should
   * execute the intent.
   *
   * @param context context to use
   * @param intent intent to resolve
   * @return {@link ActivityInfo}
   */
  static ActivityInfo resolveActivityInfo(Context context, Intent intent) {
    return intent.resolveActivityInfo(context.getPackageManager(), 0);
  }

  /**
   * Resolve a scheme url to one without parameters.
   *
   * @param schemeUrl origin scheme url
   * @return resolved scheme url
   */
  static String resolveSchemeUrl(String schemeUrl) {
    int index = checkNotNull(schemeUrl, "schemeUrl == null").lastIndexOf("?");
    return index > 0 ? schemeUrl.substring(0, index) : schemeUrl;
  }
}

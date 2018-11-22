/*
 * Copyright 2014
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.framework3eclipse.handlers;

import static com.github.framework3eclipse.creator.MethodCreatorType.TO_STRING_CREATOR;

import java.util.List;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.github.framework3eclipse.creator.MethodCreatorType;
import com.github.framework3eclipse.dto.MethodInsertionPoint;

public class ToStringHandler extends AbstractHandler {

  @Override
  public List<String> run(MethodInsertionPoint insertionPoint) throws JavaModelException {
    IType insertionType = insertionPoint.getInsertionType();
    if (!validateMethodGeneration(insertionType)) {
      return null;
    }
    List<String> fields = validateFields(insertionType);
    if (fields == null) {
      return null;
    }
    if (!checkExistingMethod(insertionType)) {
      return null;
    }
    return fields;
  };

  @Override
  public String getMethodName() {
    return "toString";
  }

  @Override
  public MethodCreatorType getMethodCreatorType() {
    return TO_STRING_CREATOR;
  }


}

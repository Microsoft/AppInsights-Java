/*
 * AppInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.agent;

import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/** Created by gupele on 8/2/2015. */
public final class RestTemplateMethodVisitor extends AbstractHttpMethodVisitor {

  public RestTemplateMethodVisitor(
      int access,
      String desc,
      String owner,
      String methodName,
      MethodVisitor methodVisitor,
      ClassToMethodTransformationData additionalData) {
    super(access, desc, owner, methodName, methodVisitor, additionalData);
  }

  @Override
  public void onMethodEnter() {
    int stringLocalIndex = this.newLocal(Type.getType(String.class));

    mv.visitVarInsn(ALOAD, 1);
    Label nullLabel = new Label();

    mv.visitJumpInsn(IFNULL, nullLabel);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/net/URI", "toString", "()Ljava/lang/String;", false);
    mv.visitVarInsn(ASTORE, stringLocalIndex);

    super.visitFieldInsn(
        GETSTATIC,
        ImplementationsCoordinator.internalName,
        "INSTANCE",
        ImplementationsCoordinator.internalNameAsJavaName);
    mv.visitLdcInsn(getMethodName());
    mv.visitVarInsn(ALOAD, stringLocalIndex);
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        ImplementationsCoordinator.internalName,
        ON_ENTER_METHOD_NAME,
        ON_ENTER_METHOD_SIGNATURE,
        false);

    Label notNullLabel = new Label();
    mv.visitJumpInsn(GOTO, notNullLabel);

    mv.visitLabel(nullLabel);

    super.visitFieldInsn(
        GETSTATIC,
        ImplementationsCoordinator.internalName,
        "INSTANCE",
        ImplementationsCoordinator.internalNameAsJavaName);
    mv.visitLdcInsn(getMethodName());
    mv.visitInsn(ACONST_NULL);

    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        ImplementationsCoordinator.internalName,
        ON_ENTER_METHOD_NAME,
        ON_ENTER_METHOD_SIGNATURE,
        false);

    mv.visitLabel(notNullLabel);
  }
}

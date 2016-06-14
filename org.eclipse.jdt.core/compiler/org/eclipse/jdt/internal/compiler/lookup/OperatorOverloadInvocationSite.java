package org.eclipse.jdt.internal.compiler.lookup;

import java.util.HashMap;

import org.eclipse.jdt.internal.compiler.ast.ExpressionContext;
import org.eclipse.jdt.internal.compiler.ast.Invocation;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;

public abstract class OperatorOverloadInvocationSite implements Invocation {

	public abstract TypeBinding getExpectedType();

	private HashMap<TypeBinding, MethodBinding> solutionsPerTargetType = null;
	private SimpleLookupTable inferenceContexts = null;

	public boolean isSuperAccess(){ return false; }
	public boolean isTypeAccess() { return true; }
	public void setActualReceiverType(ReferenceBinding actualReceiverType) { /* ignore */}
	public void setDepth(int depth) { /* ignore */}
	public void setFieldIndex(int depth){ /* ignore */}
	public int sourceStart() { return 0; }
	public int sourceEnd() { return 0; }

	@Override
	public TypeBinding[] genericTypeArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean receiverIsImplicitThis() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean checkingPotentialCompatibility() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void acceptPotentiallyCompatibleMethods(MethodBinding[] methods) {
		// TODO Auto-generated method stub
	}

	@Override
	public InferenceContext18 freshInferenceContext(Scope scope) {
		return new InferenceContext18(scope, this.arguments(), this, null);
	}

	@Override
	public ExpressionContext getExpressionContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MethodBinding binding() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerInferenceContext(ParameterizedGenericMethodBinding method, InferenceContext18 infCtx18) {
		if (this.inferenceContexts == null) {
			this.inferenceContexts = new SimpleLookupTable();
		}
		this.inferenceContexts.put(method, infCtx18);
	}

	@Override
	public InferenceContext18 getInferenceContext(ParameterizedMethodBinding method) {
		if (this.inferenceContexts == null) {
			return null;
		}
		return (InferenceContext18) this.inferenceContexts.get(method);
	}

	@Override
	public void registerResult(TypeBinding targetType, MethodBinding method) {
		if (method != null) {
			if (this.solutionsPerTargetType == null) {
				this.solutionsPerTargetType = new HashMap<TypeBinding, MethodBinding>();
			}
			this.solutionsPerTargetType.put(targetType, method);
		}
	}
	public boolean isPolyExpression(MethodBinding method) {
		// TODO Auto-generated method stub
		return false;
	}
}

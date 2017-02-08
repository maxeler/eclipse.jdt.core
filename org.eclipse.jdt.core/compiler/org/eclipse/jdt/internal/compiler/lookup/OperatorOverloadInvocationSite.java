package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.internal.compiler.ast.InnerInferenceHelper;
import org.eclipse.jdt.internal.compiler.ast.Invocation;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;

public abstract class OperatorOverloadInvocationSite implements Invocation {
 
 	public abstract TypeBinding getExpectedType();
 
 	private SimpleLookupTable inferenceContexts = null;
 	private InnerInferenceHelper innerInferenceHelper;
 	private MethodBinding binding;
 
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
 	public InferenceContext18 freshInferenceContext(Scope scope) {
 		return new InferenceContext18(scope, this.arguments(), this);
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
 	public MethodBinding binding(TypeBinding targetType, boolean reportErrors, Scope scope) {
 		if (reportErrors) {
 			if (this.binding == null)
 				scope.problemReporter().genericInferenceError("mthod is unexpectedly unresolved", this);
 		}
 		return this.binding;
 	}
 
 	@Override
 	public boolean updateBindings(MethodBinding updatedBinding, TypeBinding targetType) {
 		boolean hasUpdate = this.binding != updatedBinding;
 		if (this.inferenceContexts != null) {
 			InferenceContext18 ctx = (InferenceContext18)this.inferenceContexts.removeKey(this.binding);
 			if (ctx != null && updatedBinding instanceof ParameterizedGenericMethodBinding) {
 				this.inferenceContexts.put(updatedBinding, ctx);
 				hasUpdate |= ctx.registerSolution(targetType, updatedBinding);
 			}
 		}
 		this.binding = updatedBinding;
 
 		return hasUpdate;
 	}
 
 	@Override
 	public boolean usesInference() {
 		return (this.binding instanceof ParameterizedGenericMethodBinding) 
 			&& getInferenceContext((ParameterizedGenericMethodBinding)this.binding) != null;
 	}
 
 	@Override
 	public boolean innersNeedUpdate() {
 		return this.innerInferenceHelper != null;
 	}
 
 	@Override
 	public void innerUpdateDone() {
 		this.innerInferenceHelper = null;
 	}
 
 	@Override
 	public InnerInferenceHelper innerInferenceHelper() {
 		return this.innerInferenceHelper;
 	}
 
 	public boolean isPolyExpression(MethodBinding method) {
 		return false;
 	}
}
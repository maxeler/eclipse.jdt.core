package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.InferenceContext18;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.OperatorOverloadInvocationSite;
import org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding; 
/**
 *
 * @author milan
 *
 */
public class EqualEqualExpression extends BinaryExpression{

	public EqualEqualExpression(Expression left, Expression right, int operator) {
		super(left, right, operator);
		/**
		 * Add custom '===', '!==' code
		 */
	}

	class EqualEqualInvocationSite extends OperatorOverloadInvocationSite {
		public Expression [] siteArguments = new Expression[] { null };

		public EqualEqualInvocationSite(Expression arg) {
			siteArguments[0] = arg;
		}
		public TypeBinding getExpectedType() {
			return EqualEqualExpression.this.expectedType();
		}
		@Override
		public TypeBinding invocationTargetType() {
			return this.getExpectedType();
		}
		@Override
		public boolean receiverIsImplicitThis() {
			return EqualEqualExpression.this.receiverIsImplicitThis();
		}
		@Override
		public InferenceContext18 freshInferenceContext(Scope scope) {
			return new InferenceContext18(scope, null, this);
		}
		@Override
		public ExpressionContext getExpressionContext() {
			return EqualEqualExpression.this.getExpressionContext();
		}
		@Override
		public Expression[] arguments() {
			return siteArguments;
		}
	};

	public MethodBinding getMethodBindingForOverload(BlockScope scope) {
		TypeBinding tb_right = null;
		TypeBinding tb_left = null;

		if(this.left.resolvedType == null)
			tb_left = this.left.resolveType(scope);
		else
			tb_left = this.left.resolvedType;

		if(this.right.resolvedType == null)
			tb_right = this.right.resolveType(scope);
		else
			tb_right = this.right.resolvedType;


		String ms = ""; //$NON-NLS-1$
		String rms = ""; //$NON-NLS-1$

		if(getOperatorType() == EQUAL_EQUAL_EQUAL){
			ms = getMethodNameForEq();
			rms = getMethodNameForNeq();
		}else if(getOperatorType() == NOT_EQUAL_EQUAL){
			ms = getMethodNameForNeq();
			rms = getMethodNameForEq();
		}

		//Object <op> Object
		if (!tb_left.isBoxingType() && !tb_left.isBaseType() && !tb_right.isBoxingType() && !tb_right.isBaseType()){
			MethodBinding mbLeft = scope.getMethod(tb_left, ms.toCharArray(), new TypeBinding[]{tb_right}, new EqualEqualInvocationSite(this.right));
			MethodBinding mbRight = scope.getMethod(tb_right, (ms + "AsRHS").toCharArray(), new TypeBinding[]{tb_left}, new EqualEqualInvocationSite(this.left)); //$NON-NLS-1$
			/**
			 * Check for required counter method
			 */
			MethodBinding mbCounterLeft = scope.getMethod(tb_left, rms.toCharArray(), new TypeBinding[]{tb_right}, new EqualEqualInvocationSite(this.right));
			MethodBinding mbCounterRight = scope.getMethod(tb_right, (rms + "AsRHS").toCharArray(), new TypeBinding[]{tb_left}, new EqualEqualInvocationSite(this.left)); //$NON-NLS-1$

			if(mbLeft.isValidBinding() && mbRight.isValidBinding()){
				if(((mbLeft.modifiers & ClassFileConstants.AccStatic) != 0) && ((mbRight.modifiers & ClassFileConstants.AccStatic) != 0)) {
					scope.problemReporter().overloadedOperatorMethodNotStatic(this, ms);
					return null;
				}
				return new ProblemMethodBinding(ms.toCharArray(), new TypeBinding[]{tb_right}, ProblemReasons.Ambiguous);
			}

			if(mbLeft.isValidBinding()){
				if(!mbCounterLeft.isValidBinding())
					scope.problemReporter().invalidOrMissingOverloadedOperator(this, rms, this.right.resolvedType);

				if((mbLeft.modifiers & ClassFileConstants.AccStatic) != 0) {
					scope.problemReporter().overloadedOperatorMethodNotStatic(this, ms);
					return null;
				}
				if((mbCounterLeft.modifiers & ClassFileConstants.AccStatic) != 0) {
					scope.problemReporter().overloadedOperatorMethodNotStatic(this, rms);
					return null;
				}
				this.overloadedExpresionSide = overloadedLeftSide;
				return mbLeft;
			}

			if(mbRight.isValidBinding()){
				if(!mbCounterRight.isValidBinding())
					scope.problemReporter().invalidOrMissingOverloadedOperator(this, rms, this.right.resolvedType);

				if((mbRight.modifiers & ClassFileConstants.AccStatic) != 0) {
					scope.problemReporter().overloadedOperatorMethodNotStatic(this, ms);
					return null;
				}
				if((mbCounterRight.modifiers & ClassFileConstants.AccStatic) != 0) {
					scope.problemReporter().overloadedOperatorMethodNotStatic(this, rms);
					return null;
				}
				this.overloadedExpresionSide = overloadedRightSide;
				return mbRight;
			}
			if(tb_left.isStringType() || tb_right.isStringType()){
				return null;
			}
			return null;
		}


		//Object <op> type or type <op> Object
		if(!tb_left.isBoxingType() && !tb_left.isBaseType() && (tb_right.isBoxingType() || tb_right.isBaseType())){
			MethodBinding mbLeft = scope.getMethod(tb_left, ms.toCharArray(), new TypeBinding[]{tb_right}, new EqualEqualInvocationSite(this.right));
			MethodBinding mbCounterLeft = scope.getMethod(tb_left, rms.toCharArray(), new TypeBinding[]{tb_right}, new EqualEqualInvocationSite(this.right));
			if(mbLeft.isValidBinding() && isAnnotationSet(mbLeft)){
				if(!mbCounterLeft.isValidBinding())
					scope.problemReporter().invalidOrMissingOverloadedOperator(this, rms, this.right.resolvedType);

				if((mbLeft.modifiers & ClassFileConstants.AccStatic) != 0) {
					scope.problemReporter().overloadedOperatorMethodNotStatic(this, ms);
					return null;
				}
				if((mbCounterLeft.modifiers & ClassFileConstants.AccStatic) != 0) {
					scope.problemReporter().overloadedOperatorMethodNotStatic(this, rms);
					return null;
				}
				this.overloadedExpresionSide = overloadedLeftSide;
				return mbLeft;
			}
			return null;
		}
		if(!tb_right.isBoxingType() && !tb_right.isBaseType() && (tb_left.isBoxingType() || tb_left.isBaseType())){
			MethodBinding mbRight = scope.getMethod(tb_right, (ms + "AsRHS").toCharArray(), new TypeBinding[]{tb_left}, new EqualEqualInvocationSite(this.left)); //$NON-NLS-1$
			MethodBinding mbCounterRight = scope.getMethod(tb_right, (rms + "AsRHS").toCharArray(), new TypeBinding[]{tb_left}, new EqualEqualInvocationSite(this.left)); //$NON-NLS-1$
			if(mbRight.isValidBinding()){
				if(!mbCounterRight.isValidBinding())
					scope.problemReporter().invalidOrMissingOverloadedOperator(this, rms + "AsRHS", this.right.resolvedType);//$NON-NLS-1$

				if((mbRight.modifiers & ClassFileConstants.AccStatic) != 0) {
					scope.problemReporter().overloadedOperatorMethodNotStatic(this, ms + "AsRHS");//$NON-NLS-1$
					return null;
				}
				if((mbCounterRight.modifiers & ClassFileConstants.AccStatic) != 0) {
					scope.problemReporter().overloadedOperatorMethodNotStatic(this, rms + "AsRHS");//$NON-NLS-1$
					return null;
				}
				this.overloadedExpresionSide = overloadedRightSide;
				return mbRight;
			}
			return null;
		}
		return null;
	}

	private String getMethodNameForEq(){
		return "eq"; //$NON-NLS-1$
	}

	private String getMethodNameForNeq(){
		return "neq"; //$NON-NLS-1$
	}

	public int getOperatorType() {
		return (this.bits & ASTNode.OperatorMASK) >> ASTNode.OperatorSHIFT;
	}

}

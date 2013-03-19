/*******************************************************************************
 * Copyright (c) 2013 GK Software AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.internal.compiler.ast.Expression;

/**
 * Main class for new type inference as per JLS8 sect 18.
 * Keeps contextual state and drives the algorithm.
 */
public class InferenceContext18 {

	InferenceVariable[] inferenceVariables;
	BoundSet currentBounds;
	ConstraintFormula[] initialConstraints;
	
	Scope scope;
	LookupEnvironment environment;
	ReferenceBinding object;
	
	// interim
	Expression[] invocationArguments;
	
	/** Construct an inference context for an invocation (method/constructor). */
	public InferenceContext18(Scope scope, Expression[] arguments, TypeBinding[] genericTypeArguments) {
		this.scope = scope;
		this.environment = scope.environment();
		this.object = scope.getJavaLangObject();
		if (genericTypeArguments != null)
			createInitialBoundSet(genericTypeArguments);
		this.invocationArguments = arguments;
	}

	public void createInitialBoundSet(TypeBinding[] typeParameters) {
		// JLS8 18.1.3
		if (this.currentBounds != null) return; // already initialized from explicit type parameters
		this.currentBounds = new BoundSet();
		if (typeParameters != null) {
			addTypeVariableSubstitutions(typeParameters);
			this.currentBounds.addBoundsFromTypeParameters(this, typeParameters);
		}
	}

	public void createInitialConstraintsForParameters(TypeBinding[] parameters) {
		// 18.5.1
		// TODO discriminate (strict,loose,variable-arity) invocations
		if (this.invocationArguments == null)
			return;
		int len = Math.min(this.invocationArguments.length, parameters.length); // varargs
		this.initialConstraints = new ConstraintFormula[len];
		for (int i = 0; i < len; i++) {
			TypeBinding thetaF = substitute(parameters[i]);	
			this.initialConstraints[i] = new ConstraintExpressionFormula(this.invocationArguments[i], thetaF, ReductionResult.COMPATIBLE);
		}
	}

	public void createInitialConstraintsForTargetType(TypeBinding returnType, TypeBinding expectedType) {
		// 18.5.2
// TODO: should depend on isPolyExpression once that is reliably in place:
		if (expectedType == null) return;
// TODO: enable once isPolyExpression is reliably in place
//		if (returnType == TypeBinding.VOID) this.currentBounds.isFalse = true;
		TypeBinding thetaR = substitute(returnType);
		if (expectedType.isBaseType()) {
			if (thetaR instanceof InferenceVariable) {
				TypeBinding wrapper = this.currentBounds.findWrapperTypeBound((InferenceVariable)thetaR);
				if (wrapper != null) {
					int l = 0;
					if (this.initialConstraints != null) {
						l = this.initialConstraints.length;
						System.arraycopy(this.initialConstraints, 0, this.initialConstraints=new ConstraintFormula[l+2], 0, l);
					} else {
						l = 0;
						this.initialConstraints = new ConstraintFormula[2];
					}
					this.initialConstraints[l] = new ConstraintTypeFormula(thetaR, wrapper, ReductionResult.SAME);
					this.initialConstraints[l+1] = new ConstraintTypeFormula(wrapper, expectedType, ReductionResult.COMPATIBLE);
					return;
				}
			}
// TODO: could be OK if !isPolyExpression:
//			InferenceContext18.missingImplementation("NYI");
//			return;
		}
		int l = 0;
		if (this.initialConstraints != null) {
			l = this.initialConstraints.length;
			System.arraycopy(this.initialConstraints, 0, this.initialConstraints=new ConstraintFormula[l+1], 0, l);
		} else {
			l = 0;
			this.initialConstraints = new ConstraintFormula[1];
		}
		this.initialConstraints[l] = new ConstraintTypeFormula(thetaR, expectedType, ReductionResult.COMPATIBLE);
	}

	public InferenceVariable createInferenceVariable(ReferenceBinding type) {
		InferenceVariable variable;
		if (this.inferenceVariables == null) {
			variable = new InferenceVariable(type, 0);
			this.inferenceVariables = new InferenceVariable[] { variable };
		} else {
			int len = this.inferenceVariables.length;
			variable = new InferenceVariable(type, len);
			System.arraycopy(this.inferenceVariables, 0, this.inferenceVariables = new InferenceVariable[len+1], 0, len);
			this.inferenceVariables[len] = variable;
		}
		return variable;
	}

	public void addTypeVariableSubstitutions(TypeBinding[] typeVariables) {
		int len2 = typeVariables.length;
		int start = 0;
		if (this.inferenceVariables != null) {
			int len1 = this.inferenceVariables.length;
			System.arraycopy(this.inferenceVariables, 0, this.inferenceVariables = new InferenceVariable[len1+len2], 0, len1);
			start = len1;
		} else {
			this.inferenceVariables = new InferenceVariable[len2];
		}
		for (int i = 0; i < typeVariables.length; i++)
			this.inferenceVariables[start+i] = new InferenceVariable(typeVariables[i], start+i);
	}

	/**
	 * Try to solve the inference problem defined by constraints and bounds previously registered.
	 * @return success?
	 */
	public boolean solve(boolean includeProvisional) {
		if (!reduce())
			return false;
		if (!this.currentBounds.incorporate(this))
			return false;
		if (!resolve())
			return false;
		return isResolved() || (includeProvisional && hasDelayedConstraints());
	}

	/**
	 * JLS 18.2. reduce all initial constraints 
	 */
	boolean reduce() {
		if (this.initialConstraints != null) {
			for (int i = 0; i < this.initialConstraints.length; i++) {
				if (!this.currentBounds.reduceOneConstraint(this, this.initialConstraints[i]))
					return false;
			}
		}
		return true;
	}
	
	/**
	 * <b>JLS 18.4</b> Resolution
	 * @return answer false if some constraint resolved to FALSE, true otherwise.
	 */
	private boolean resolve() {
		if (this.inferenceVariables != null) {
			for (int i = 0; i < this.inferenceVariables.length; i++) {
				if (this.currentBounds.isInstantiated(this.inferenceVariables[i])) continue;
				// find a minimal set of dependent variables:
				Set variableSet = new HashSet();
				int numUninstantiated = addDependencies(variableSet, i);
				int numVars = variableSet.size();
				
				if (numUninstantiated > 0 && numVars > 0) {
					// try to instantiate this set of variables in a fresh copy of the bound set:
					BoundSet oldBoundSet = this.currentBounds;
					this.currentBounds = this.currentBounds.copy();
					InferenceVariable[] variables = (InferenceVariable[]) variableSet.toArray(new InferenceVariable[numVars]);
					for (int j = 0; j < variables.length; j++) {
						InferenceVariable variable = variables[j];
						// try lower bounds:
						TypeBinding[] lowerBounds = this.currentBounds.lowerBounds(variable);
						if (lowerBounds != Binding.NO_TYPES) {
							TypeBinding lub = this.scope.lowerUpperBound(lowerBounds);
							if (lub != TypeBinding.VOID && lub != null)
								this.currentBounds.addBound(new TypeBound(variable, lub, ReductionResult.SAME));
							continue;
						}
						// try upper bounds:
						TypeBinding[] upperBounds = this.currentBounds.upperBounds(variable);
						if (upperBounds != Binding.NO_TYPES) {
							TypeBinding glb;
							if (upperBounds.length == 1) {
								glb = upperBounds[0];
							} else {
								ReferenceBinding[] glbs = Scope.greaterLowerBound((ReferenceBinding[])upperBounds);
								if (glbs == null)
									throw new UnsupportedOperationException("no glb for "+Arrays.asList(upperBounds));
								else if (glbs.length == 1)
									glb = glbs[0];
								else
									glb = new IntersectionCastTypeBinding(glbs, this.environment);
							}
							this.currentBounds.addBound(new TypeBound(variable, glb, ReductionResult.SAME));
						}
					}
					if (!this.currentBounds.incorporate(this)) {
						this.currentBounds = oldBoundSet;
						// TODO: implement "second attempt" from 18.4
						return false; // FIXME ensure all effects reverted / discarded
					}
				}
			}
		}
		return true;
	}
	
	/** 
	 * starting with our i'th inference variable collect all variables
	 * reachable via dependencies (regardless of relation kind).
	 * @param variableSet collect all variables found into this set
	 * @param i seed index into {@link #inferenceVariables}.
	 * @return count of uninstantiated variables in the set.
	 */
	private int addDependencies(Set variableSet, int i) {
		InferenceVariable currentVariable = this.inferenceVariables[i];
		if (this.currentBounds.isInstantiated(currentVariable)) return 0;
		if (!variableSet.add(currentVariable)) return 1;
		int numUninstantiated = 1;
		for (int j = 0; j < this.inferenceVariables.length; j++) {
			if (i == j) continue;
			if (this.currentBounds.dependsOnResolutionOf(currentVariable, this.inferenceVariables[j]))
				numUninstantiated += addDependencies(variableSet, j);
		}
		return numUninstantiated;
	}

	/**
	 * Substitute any type variables mentioned in 'type' by the corresponding inference variable, if one exists. 
	 */
	public TypeBinding substitute(TypeBinding type) {
		return 	Scope.substitute(new InferenceSubstitution(this.environment, this.inferenceVariables), type);
	}

	/**
	 * Have all inference variables been instantiated successfully?
	 */
	public boolean isResolved() {
		if (this.inferenceVariables != null) {
			for (int i = 0; i < this.inferenceVariables.length; i++) {
				if (!this.currentBounds.isInstantiated(this.inferenceVariables[i]))
					return false;
			}
		}
		return true;
	}
	
	public boolean hasDelayedConstraints() {
		return this.currentBounds.constraintCount > 0;
	}

	/**
	 * Retrieve the resolved solutions for all given type variables.
	 * @param typeParameters
	 * @return the substituted types or <code>null</code> if any type variable could not be substituted.
	 */
	public TypeBinding /*@Nullable*/[] getSolutions(final TypeVariableBinding[] typeParameters) {
		int len = typeParameters.length;
		TypeBinding[] substitutions = new TypeBinding[len];
		for (int i = 0; i < typeParameters.length; i++) {
			for (int j = 0; j < this.inferenceVariables.length; j++) {
				InferenceVariable variable = this.inferenceVariables[j];
				if (variable.typeParameter == typeParameters[i]) {
					substitutions[i] = this.currentBounds.getInstantiation(variable);
					break;
				}
			}
			if (substitutions[i] == null)
				return null;
		}
		return substitutions;
	}

	// debugging:
	public String toString() {
		StringBuffer buf = new StringBuffer("Inference Context"); //$NON-NLS-1$
		if (isResolved())
			buf.append(" (resolved)"); //$NON-NLS-1$
		buf.append('\n');
		if (this.inferenceVariables != null) {
			buf.append("Inference Variables:\n"); //$NON-NLS-1$
			for (int i = 0; i < this.inferenceVariables.length; i++) {
				buf.append('\t').append(this.inferenceVariables[i].sourceName).append("\t:\t"); //$NON-NLS-1$
				if (this.currentBounds.isInstantiated(this.inferenceVariables[i]))
					buf.append(this.currentBounds.getInstantiation(this.inferenceVariables[i]).readableName());
				else
					buf.append("NOT INSTANTIATED"); //$NON-NLS-1$
				buf.append('\n');
			}
		}
		if (this.initialConstraints != null) {
			buf.append("Initial Constraints:\n"); //$NON-NLS-1$
			for (int i = 0; i < this.initialConstraints.length; i++)
				if (this.initialConstraints[i] != null)
					buf.append('\t').append(this.initialConstraints[i].toString()).append('\n');
		}
		if (this.currentBounds != null)
			buf.append(this.currentBounds.toString());
		return buf.toString();
	}

	// INTERIM: infrastructure for detecting failures caused by specific known incompleteness:
	static final String JLS_18_2_3_INCOMPLETE_TO_DO_DEFINE_THE_MOST_SPECIFIC_ARRAY_SUPERTYPE_OF_A_TYPE_T = "JLS 18.2.3 incomplete: \"To do: [...] define the most specific array supertype of a type T.\""; //$NON-NLS-1$
	static final String JLS_18_2_3_INCOMPLETE_TO_DEFINE_THE_PARAMETERIZATION_OF_A_CLASS_C_FOR_A_TYPE_T = "JLS 18.2.3 incomplete: \"To do: define the parameterization of a class C for a type T\""; //$NON-NLS-1$
	public static void missingImplementation(String msg) {
		if (msg == JLS_18_2_3_INCOMPLETE_TO_DO_DEFINE_THE_MOST_SPECIFIC_ARRAY_SUPERTYPE_OF_A_TYPE_T)
			return; // without this return we see 56 distinct errors in GenericTypeTest
		if (msg == JLS_18_2_3_INCOMPLETE_TO_DEFINE_THE_PARAMETERIZATION_OF_A_CLASS_C_FOR_A_TYPE_T)
			return; // without this return we see 54 distinct errors in GenericTypeTest
		throw new UnsupportedOperationException(msg);
	}
}
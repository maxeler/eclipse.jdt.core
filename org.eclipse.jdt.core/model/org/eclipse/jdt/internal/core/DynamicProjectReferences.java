package org.eclipse.jdt.internal.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IDynamicReferenceProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class DynamicProjectReferences implements IDynamicReferenceProvider {
	@Override
	public List<IProject> getDependentProjects(IBuildConfiguration buildConfiguration) throws CoreException {
		IProject input = buildConfiguration.getProject();
		IJavaProject javaProject = JavaCore.create(input);
		if (javaProject instanceof JavaProject) {
			JavaProject project = (JavaProject) javaProject;

			String[] required = project.projectPrerequisites(project.getResolvedClasspath());

			IWorkspaceRoot wksRoot = input.getWorkspace().getRoot();
			return Arrays.stream(required).sorted().map(name -> wksRoot.getProject(name)).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}
}

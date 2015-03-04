/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.forge.camel.commands.project;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.URLResource;
import org.jboss.forge.addon.templates.Template;
import org.jboss.forge.addon.templates.TemplateFactory;
import org.jboss.forge.addon.templates.freemarker.FreemarkerTemplate;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

@FacetConstraint({ResourcesFacet.class})
public class CamelAddBlueprintXml extends AbstractCamelProjectCommand {

    @Inject
    @WithAttributes(label = "directory", required = false, defaultValue = "OSGI-INF/blueprint",
            description = "The directory name where this type will be created")
    private UIInput<String> directory;

    @Inject
    @WithAttributes(label = "name", required = true, description = "Name of XML file")
    private UIInput<String> name;

    @Inject
    private TemplateFactory factory;

    @Inject
    ResourceFactory resourceFactory;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelAddRouteBuilder.class).name(
                "project-camel-add-blueprint-xml").category(Categories.create(CATEGORY))
                .description("Adds a Blueprint XML file with CamelContext included to your project");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        name.addValidator(new ResourceNameValidator("xml"));
        builder.add(directory).add(name);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);
        String projectName = project.getRoot().getName();

        String fileName = directory.getValue() != null ? directory.getValue() + File.separator + name.getValue() : name.getValue();
        String fullName = "src" + File.separator + "main" + File.separator + "resources" + File.separator + fileName;

        ResourcesFacet facet = project.getFacet(ResourcesFacet.class);
        // this will get a file in the src/main/resources directory where we want to store the spring xml file
        FileResource<?> fileResource = facet.getResource(fileName);

        if (fileResource.exists()) {
            return Results.fail("Blueprint XML file " + fullName + " already exists");
        }

        Resource<URL> xml = resourceFactory.create(getClass().getResource("/templates/camel-blueprint.ftl")).reify(URLResource.class);
        Template template = factory.create(xml, FreemarkerTemplate.class);

        // any dynamic options goes into the params map
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("projectName", projectName);
        String output = template.process(params);

        // create the new file and set the content
        fileResource.createNewFile();
        fileResource.setContents(output);

        return Results.success("Added Blueprint XML file " + fullName + " to the project");
    }
}

/**
 * This file Copyright (c) 2018 Magnolia International
 * Ltd.  (http://www.magnolia-cms.com). All rights reserved.
 *
 *
 * This program and the accompanying materials are made
 * available under the terms of the Magnolia Network Agreement
 * which accompanies this distribution, and is available at
 * http://www.magnolia-cms.com/mna.html
 *
 * Any modifications to this file must keep this entire header
 * intact.
 *
 */
package info.magnolia.development.rest.services.v1;

import javax.ws.rs.Path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import info.magnolia.config.registry.DefinitionMetadata;
import info.magnolia.config.registry.DefinitionProvider;
import info.magnolia.config.registry.DefinitionQuery;
import info.magnolia.config.registry.RegistryFacade;
import info.magnolia.config.registry.DefinitionProvider.Problem;
import info.magnolia.config.registry.DefinitionProvider.Problem.SeverityType;
import info.magnolia.definitions.app.problems.ProblemFilterContext;
import info.magnolia.module.ModuleRegistry;
import info.magnolia.objectfactory.Components;
import info.magnolia.rendering.template.TemplateDefinition;
import info.magnolia.rendering.template.registry.TemplateDefinitionRegistry;
import info.magnolia.rest.AbstractEndpoint;
import info.magnolia.rest.EndpointDefinition;
import info.magnolia.ui.api.app.AppDescriptor;
import info.magnolia.ui.api.app.registry.AppDescriptorRegistry;
import info.magnolia.ui.dialog.definition.DialogDefinition;
import info.magnolia.ui.dialog.registry.DialogDefinitionRegistry;
import info.magnolia.ui.form.fieldtype.definition.FieldTypeDefinition;
import info.magnolia.ui.form.fieldtype.registry.FieldTypeDefinitionRegistry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cedarsoftware.util.io.JsonWriter;

/**
 * Rest endpoint for accessing the registries of Magnolia.
 * 
 * @author rgange
 *
 * @param <D>
 */
@Path("/registry/v1")
public class RegistryEndpoint<D extends EndpointDefinition> extends AbstractEndpoint<D> {
    
    private final static Logger log = LoggerFactory.getLogger(RegistryEndpoint.class);
    
    public RegistryEndpoint(D endpointDefinition) {
        super(endpointDefinition);
    }
    
    @Path("/{registry}{module:(/.+)?}")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response data(
            @PathParam("registry") String registry,
            @PathParam("module") @DefaultValue(StringUtils.EMPTY) String module,
            @QueryParam("type") @DefaultValue(StringUtils.EMPTY) String type,
            @QueryParam("onlyDefinitionProviders") @DefaultValue("false") String onlyDefinitionProviders) {
        
        String json = StringUtils.EMPTY;
        List<String> requestedData = new ArrayList<>();
        module = StringUtils.isNotEmpty(module) ? module.substring(1) : StringUtils.EMPTY; // trim the slash
        
        switch (registry) {
            case "modules":
                
                // check to see if a specific module was requested in the path
                if (StringUtils.isNotEmpty(module)) {
                    
                    // if the type param is set then return all definitions of the type for the specific module
                    if (StringUtils.isNotEmpty(type)) {
                        @SuppressWarnings({ "rawtypes", "unchecked" })
                        Collection<DefinitionProvider> definitionProviders = 
                            Components.getComponent(RegistryFacade.class).query().from(module).findMultiple();
                        
                        for (DefinitionProvider<?> definitionProvider : definitionProviders) {
                            DefinitionMetadata metadata = definitionProvider.getMetadata();
                            if (metadata != null && metadata.getModule() != null && type.equals((metadata.getType().getPluralName())))
                                // return the relative location which includes any sub-folders that might
                                // exist in the requested registry type
                                requestedData.add(metadata.getRelativeLocation()); 
                        }
                    }
                    
                    // else return a list of registires which provide definitions for the specific module
                    else {
                        @SuppressWarnings({ "rawtypes" })
                        Collection<DefinitionProvider> definitionProviders = 
                            Components.getComponent(RegistryFacade.class).byModule(module);
                        
                        for (DefinitionProvider<?> definitionProvider : definitionProviders) {
                            DefinitionMetadata metadata = definitionProvider.getMetadata();
                            if (metadata != null && metadata.getModule() != null && !requestedData.contains(metadata.getType().getPluralName()))
                                requestedData.add(metadata.getType().getName()); 
                        }
                    }
                }
                
                // return a list of modules which have definitions
                else if (Boolean.valueOf(onlyDefinitionProviders)) {
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    Collection<DefinitionProvider> definitionProviders = 
                        Components.getComponent(RegistryFacade.class).query().findMultiple();
                    
                    for (DefinitionProvider<?> definitionProvider : definitionProviders) {
                        DefinitionMetadata metadata = definitionProvider.getMetadata();
                        if (metadata != null && metadata.getModule() != null && !requestedData.contains(metadata.getModule()))
                            requestedData.add(metadata.getModule()); 
                    }
                }
                
                // return a complete list of all modules
                else {
                    ModuleRegistry moduleRegistry = Components.getComponent(ModuleRegistry.class);
                    Iterator<String> moduleNamesIterator = moduleRegistry.getModuleNames().iterator();
                    
                    while (moduleNamesIterator.hasNext()) 
                        requestedData.add(moduleNamesIterator.next());
                }
                break;
                
            case "apps":
                Iterator<AppDescriptor> appDescriptorIterator = 
                    Components.getComponent(AppDescriptorRegistry.class).getAllDefinitions().iterator();
                
                while (appDescriptorIterator.hasNext()) 
                    requestedData.add(appDescriptorIterator.next().getName());
                break;
                
            case "templates":
                Iterator<TemplateDefinition> templateDefinitionIterator = 
                        Components.getComponent(TemplateDefinitionRegistry.class).getAllDefinitions().iterator();
                
                while (templateDefinitionIterator.hasNext()) 
                    requestedData.add(templateDefinitionIterator.next().getId());
                break;
                
            case "fields":
                Iterator<FieldTypeDefinition> fieldTypeDefinitionIterator = 
                        Components.getComponent(FieldTypeDefinitionRegistry.class).getAllDefinitions().iterator();
            
                while (fieldTypeDefinitionIterator.hasNext()) 
                    requestedData.add(fieldTypeDefinitionIterator.next().getDefinitionClass().getName());
                break;
                
            case "dialogs":
                Iterator<DialogDefinition> dialogDefinitionIterator = 
                        Components.getComponent(DialogDefinitionRegistry.class).getAllDefinitions().iterator();
                
                while (dialogDefinitionIterator.hasNext()) 
                    requestedData.add(dialogDefinitionIterator.next().getId());
                break;
                
            case "problems":
                RegistryFacade registryFacade = Components.getComponent(RegistryFacade.class);
                ProblemFilterContext context = new ProblemFilterContext(registryFacade.query(), SeverityType.MINOR);
                @SuppressWarnings("rawtypes")
                DefinitionQuery query = context.getApplicableDefinitions();
                @SuppressWarnings("unchecked")
                Collection<DefinitionProvider<?>> problematicDefinitionProviders = query.findMultiple();
                
                for (DefinitionProvider<?> definitionProvider : problematicDefinitionProviders) {
                    Collection<Problem> problemIds = definitionProvider.getProblems();

                    for (Problem problem : problemIds)
                        requestedData.add(problem.getTitle());
                }
                break;
            
            default: log.error("Either the registry [{}] is not supported or does not exist.", registry);
        }
        
        Collections.sort(requestedData);
        json = JsonWriter.objectToJson(requestedData.toArray());
        return Response.status(Response.Status.OK).entity(json).build();     
    }
}

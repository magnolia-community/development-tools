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

import java.util.Arrays;

import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.magnolia.cms.security.Role;
import info.magnolia.cms.security.RoleManager;
import info.magnolia.cms.security.SecuritySupport;
import info.magnolia.context.MgnlContext;
import info.magnolia.jcr.util.NodeTypeTemplateUtil;
import info.magnolia.jcr.util.NodeTypes;
import info.magnolia.objectfactory.Components;
import info.magnolia.repository.RepositoryManager;
import info.magnolia.rest.AbstractEndpoint;
import info.magnolia.rest.EndpointDefinition;

/**
 * Rest endpoint for manipulating the repository.
 * 
 * @author rgange
 *
 * @param <D>
 */
@Path("/repository/v1")
public class RepositoryEndpoint<D extends EndpointDefinition> extends AbstractEndpoint<D> {
    
    private final static Logger log = LoggerFactory.getLogger(RepositoryEndpoint.class);
    
    public RepositoryEndpoint(D endpointDefinition) {
        super(endpointDefinition);
    }
    
    @Path("/workspace/{name}")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response createWorkspace(@PathParam("name") String name) {
        
        // if plural create a nodeType name singular
        String nameSingular = name;
        if (nameSingular.endsWith("s")) 
            nameSingular = name.substring(0, name.length()-1);
        
        try {
            // create the workspace and the node type
            Components.getComponent(RepositoryManager.class).createWorkspace("magnolia", name);
            Session appSession = MgnlContext.getJCRSession(name);
            NodeTypeManager nodeTypeManager = appSession.getWorkspace().getNodeTypeManager();
            NodeTypeTemplate type = NodeTypeTemplateUtil.createSimpleNodeType(nodeTypeManager, nameSingular, 
                    Arrays.asList(
                        NodeType.NT_HIERARCHY_NODE, 
                        NodeType.MIX_REFERENCEABLE, 
                        NodeTypes.Created.NAME, 
                        NodeTypes.Activatable.NAME, 
                        NodeTypes.LastModified.NAME, 
                        NodeTypes.Renderable.NAME
                    )
            );
            nodeTypeManager.registerNodeType(type, true);
            appSession.save();
            nodeTypeManager.getNodeType(nameSingular);
            
            log.info("Workspace {} created", name);
            log.info("Node type {} registered", nameSingular);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        
        try {            
            // set up security
            RoleManager roleManager = Components.getComponent(SecuritySupport.class).getRoleManager();
            // add superuser permission to edit the content of the app
            roleManager.addPermission(roleManager.getRole("superuser"), name, "/", 63);
            roleManager.addPermission(roleManager.getRole("superuser"), name, "/*", 63);
            
            // create a base role w/ read access
            Role base = roleManager.createRole(name + "-base");
            roleManager.addPermission(base, name, "/", 8);
            roleManager.addPermission(base, name, "/*", 8);
            
            // create an editor role w/ write access
            base = roleManager.createRole(name + "-editor");
            roleManager.addPermission(base, name, "/", 63);
            roleManager.addPermission(base, name, "/*", 63);
            
            log.info("Role {}-base created with read access.", name);
            log.info("Role {}-editor created with write access.", name);
            
        } catch (Exception e) {
            log.error("Unable to setup {} app security. Configure the permissions using the Security app.", name);
            log.error(e.getMessage(), e);
        }
        
        return Response.status(Response.Status.OK).entity("Workspace " + name + " created").build();
    }
}

/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.platform.dataaccess.datasource.api;

import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.pentaho.metadata.model.Domain;
import org.pentaho.metadata.model.LogicalModel;
import org.pentaho.metadata.repository.IMetadataDomainRepository;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.action.mondrian.catalog.IMondrianCatalogService;
import org.pentaho.platform.security.policy.rolebased.actions.AdministerSecurityAction;
import org.pentaho.platform.security.policy.rolebased.actions.RepositoryCreateAction;
import org.pentaho.platform.security.policy.rolebased.actions.RepositoryReadAction;

public class DatasourceService {

  protected IMetadataDomainRepository metadataDomainRepository;
  protected IMondrianCatalogService mondrianCatalogService;

  public DatasourceService() {
    metadataDomainRepository = PentahoSystem.get( IMetadataDomainRepository.class, PentahoSessionHolder.getSession() );
    mondrianCatalogService = PentahoSystem.get( IMondrianCatalogService.class, PentahoSessionHolder.getSession() );
  }

  public static boolean canAdminister() {
    IAuthorizationPolicy policy = PentahoSystem.get( IAuthorizationPolicy.class );
    return policy.isAllowed( RepositoryReadAction.NAME ) && policy.isAllowed( RepositoryCreateAction.NAME )
        && ( policy.isAllowed( AdministerSecurityAction.NAME ) );
  }

  /**
   * Fix for "%5C" and "%2F" in datasource name ("/" and "\" are omitted and %5C, %2F are decoded in
   * PentahoPathDecodingFilter.EncodingAwareHttpServletRequestWrapper)
   *
   * @param param
   *          pathParam
   * @return correct param
   */
  protected String fixEncodedSlashParam( String param ) {
    return param.replaceAll( "\\\\", "%5C" ).replaceAll( "/", "%2F" );
  }

  protected boolean isMetadataDatasource( String id ) {
    Domain domain;
    try {
      domain = metadataDomainRepository.getDomain( id );
      if ( domain == null )
        return false;
    } catch ( Exception e ) { // If we can't load the domain then we MUST return false
      return false;
    }

    List<LogicalModel> logicalModelList = domain.getLogicalModels();
    if ( logicalModelList != null && logicalModelList.size() >= 1 ) {
      for ( LogicalModel logicalModel : logicalModelList ) {
        // keep this check for backwards compatibility for now
        Object property = logicalModel.getProperty( "AGILE_BI_GENERATED_SCHEMA" ); //$NON-NLS-1$
        if ( property != null ) {
          return false;
        }

        // moving forward any non metadata generated datasource should have this property
        property = logicalModel.getProperty( "WIZARD_GENERATED_SCHEMA" ); //$NON-NLS-1$
        if ( property != null ) {
          return false;
        }
      }
      return true;
    } else {
      return true;
    }
  }

  public class UnauthorizedAccessException extends Exception {
    public Status getStatus() {
      return UNAUTHORIZED;
    }
  }
}
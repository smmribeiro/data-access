/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.platform.dataaccess.datasource.wizard.sources.multitable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.pentaho.agilebi.modeler.models.JoinRelationshipModel;
import org.pentaho.agilebi.modeler.models.JoinTableModel;
import org.pentaho.database.model.IDatabaseConnection;
import org.pentaho.metadata.model.Domain;
import org.pentaho.platform.dataaccess.datasource.wizard.AbstractWizardStep;
import org.pentaho.platform.dataaccess.datasource.wizard.controllers.MessageHandler;
import org.pentaho.platform.dataaccess.datasource.wizard.models.IWizardModel;
import org.pentaho.platform.dataaccess.datasource.wizard.service.impl.JoinSelectionServiceGwtImpl;
import org.pentaho.platform.dataaccess.datasource.wizard.service.impl.MultiTableDatasourceDTO;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulException;
import org.pentaho.ui.xul.XulServiceCallback;
import org.pentaho.ui.xul.binding.BindingConvertor;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.components.XulMenuList;
import org.pentaho.ui.xul.containers.XulListbox;
import org.pentaho.ui.xul.containers.XulVbox;
import org.pentaho.ui.xul.gwt.binding.GwtBindingFactory;
import org.pentaho.ui.xul.stereotype.Bindable;
import org.pentaho.ui.xul.util.AbstractModelList;

@SuppressWarnings( "all" )
public class TablesSelectionStep extends AbstractWizardStep {

  protected static final String JOIN_STEP_PANEL_ID = "joinSelectionWindow";

  private XulVbox tablesSelectionDialog;
  private XulListbox availableTables;
  private XulListbox selectedTables;
  private XulMenuList<JoinTableModel> factTables;
  private XulMenuList<String> schemas;
  private MultitableGuiModel joinGuiModel;
  private JoinSelectionServiceGwtImpl joinSelectionServiceGwtImpl;
  private SchemaSelection schemaSelection;
  private MultiTableDatasource mtdatasource;
  private Domain domain;
  private MultiTableDatasourceDTO datasourceDTO;

  public TablesSelectionStep( MultitableGuiModel joinGuiModel, JoinSelectionServiceGwtImpl joinSelectionServiceGwtImpl,
                              MultiTableDatasource parentDatasource ) {
    super( parentDatasource );
    this.joinGuiModel = joinGuiModel;
    this.joinSelectionServiceGwtImpl = joinSelectionServiceGwtImpl;
    this.schemaSelection = new SchemaSelection();
    this.mtdatasource = parentDatasource;
  }

  public String getName() {
    return "joinSelectionStepController";
  }

  public void retrieveSchemas( final IDatabaseConnection connection ) {
    joinSelectionServiceGwtImpl.retrieveSchemas( connection, new XulServiceCallback<List>() {
      public void error( String message, Throwable error ) {
        error.printStackTrace();
        mtdatasource.displayErrors( new JoinError( message, error.getMessage() ) );
      }

      public void success( List schemaValues ) {
        schemas.removePropertyChangeListener( schemaSelection );
        joinGuiModel.setSchemas( schemaValues );
        //processAvailableTables(connection, schemaValues.size() > 0 ? schemaValues.get(0).toString() : null);
        schemas.addPropertyChangeListener( schemaSelection );
      }
    } );
  }

  private void processAvailableTables( IDatabaseConnection connection, String schema ) {

    joinSelectionServiceGwtImpl.getDatabaseTables( connection, schema, new XulServiceCallback<List>() {
      public void error( String message, Throwable error ) {
        error.printStackTrace();
        mtdatasource.displayErrors( new JoinError( message, error.getMessage() ) );
        closeWaitingDialog();
      }

      public void success( List tables ) {
        try {
          if ( tables.size() == 0 ) {
            return;
          }
          if ( domain != null && datasourceDTO != null ) {
            joinGuiModel.populateJoinGuiModel( domain, datasourceDTO, tables );
            if ( joinGuiModel.getFactTable() != null ) {
              setFactTable( joinGuiModel.getFactTable() );
            }
          }
          joinGuiModel.processAvailableTables( tables );
        } finally {
          closeWaitingDialog();
        }
      }
    } );
  }

  @Bindable
  public void addSelectedTable() {
    if ( this.availableTables.getSelectedItem() != null ) {
      List<JoinTableModel> selected = new ArrayList<JoinTableModel>();
      for ( Object obj : this.availableTables.getSelectedItems() ) {
        selected.add( (JoinTableModel) obj );
      }
      this.joinGuiModel.addSelectedTables( selected );

    }
    checkValidState();
  }

  @Bindable
  public void removeSelectedTable() {
    if ( this.selectedTables.getSelectedItem() != null ) {

      List<JoinTableModel> selected = new ArrayList<JoinTableModel>();
      for ( Object obj : this.selectedTables.getSelectedItems() ) {
        selected.add( (JoinTableModel) obj );
      }
      this.joinGuiModel.removeSelectedTables( selected );

    }
    checkValidState();
    checkExistingJoinsStillValid();
  }

  private void checkExistingJoinsStillValid() {
    Set<String> allTables = new HashSet<String>();
    for ( JoinTableModel tbl : joinGuiModel.getSelectedTables() ) {
      allTables.add( tbl.getName() );
    }

    List<JoinRelationshipModel> toRemove = new ArrayList<JoinRelationshipModel>();
    for ( JoinRelationshipModel join : joinGuiModel.getJoins() ) {
      if ( !allTables.contains( join.getLeftKeyFieldModel().getParentTable().getName() ) || !allTables
        .contains( join.getRightKeyFieldModel().getParentTable().getName() ) ) {
        toRemove.add( join );
      }
    }
    for ( JoinRelationshipModel join : toRemove ) {
      joinGuiModel.getJoins().remove( join );
    }
  }

  @Override
  public void init( IWizardModel wizardModel ) throws XulException {
    this.tablesSelectionDialog = (XulVbox) document.getElementById( JOIN_STEP_PANEL_ID );
    this.availableTables = (XulListbox) document.getElementById( "availableTables" );
    this.selectedTables = (XulListbox) document.getElementById( "selectedTables" );
    this.factTables = (XulMenuList<JoinTableModel>) document.getElementById( "factTables" );
    this.schemas = (XulMenuList<String>) document.getElementById( "schemas" );
    super.init( wizardModel );
  }

  public void setBindings() {
    BindingFactory bf = new GwtBindingFactory( document );
    bf.createBinding( this.joinGuiModel.getAvailableTables(), "children", this.availableTables, "elements" );
    bf.createBinding( this.joinGuiModel.getSelectedTables(), "children", this.selectedTables, "elements" );
    bf.createBinding( this.joinGuiModel.getSelectedTables(), "children", this.factTables, "elements",
      new BindingConvertor<AbstractModelList<JoinTableModel>, Collection<JoinTableModel>>() {

        @Override
        public Collection<JoinTableModel> sourceToTarget( AbstractModelList<JoinTableModel> list ) {
          List<JoinTableModel> tables = new ArrayList<JoinTableModel>();
          tables.addAll( list.asList() );
          JoinTableModel emptyOption = new JoinTableModel();
          emptyOption.setName( MessageHandler.getString( "multitable.SELECT_TABLE" ) );
          tables.add( 0, emptyOption ); //Empty option must be always 0.
          return tables;
        }

        @Override
        public AbstractModelList<JoinTableModel> targetToSource( final Collection<JoinTableModel> list ) {
          return null;
        }
      } );

    bf.createBinding( this.joinGuiModel, "schemas", this.schemas, "elements",
      new BindingConvertor<List<String>, Collection<String>>() {

        @Override
        public Collection<String> sourceToTarget( List<String> list ) {
          List<String> tables = new ArrayList<String>();
          tables.addAll( list );
          return tables;
        }

        @Override
        public List<String> targetToSource( final Collection<String> list ) {
          return null;
        }
      } );

    bf.createBinding( this.factTables, "selectedIndex", this.joinGuiModel, "factTable",
      new BindingConvertor<Integer, JoinTableModel>() {

        @Override
        public JoinTableModel sourceToTarget( final Integer index ) {
          if ( index == -1 ) {
            return null;
          }
          //Index 0 represents [select table] option.
          //To be valid index must not be 0.
          checkValidState();
          int i = (int) index;
          i--;
          return i < 0 ? null : joinGuiModel.getSelectedTables().get( i );
        }

        @Override
        public Integer targetToSource( final JoinTableModel value ) {
          return joinGuiModel.getSelectedTables().indexOf( value );
        }
      } );

    // use a binding to handle the visibility state of the schema type radio group
    try {
      bf.createBinding( joinGuiModel, "doOlap", "factTableVbox", "visible" ).fireSourceChanged();
    } catch ( Exception e ) {
      e.printStackTrace();
      mtdatasource.displayErrors( new JoinError( e.getMessage(), e.getMessage() ) );
    }
  }

  public String getStepName() {
    return MessageHandler.getString( "multitable.SELECT_TABLES" );
  }

  public XulComponent getUIComponent() {
    return this.tablesSelectionDialog;
  }

  public void setFactTable( JoinTableModel factTable ) {
    List<JoinTableModel> tables = new ArrayList<JoinTableModel>();
    tables.addAll( this.factTables.getElements() );
    if ( factTable != null ) {
      this.factTables.setSelectedIndex( tables.indexOf( factTable.getName() ) );
    }
  }

  @Override
  public void stepActivatingReverse() {
    super.stepActivatingReverse();
    parentDatasource.setFinishable( false );

    checkValidState();
  }

  private void checkValidState() {
    boolean valid = true;
    boolean finishable = true;
    if ( joinGuiModel.isDoOlap() ) {
      valid &= this.factTables.getSelectedIndex() > 0;
      finishable &= this.factTables.getSelectedIndex() > 0;
    }
    valid &= this.selectedTables.getElements().size() > 1;
    finishable &= this.selectedTables.getElements().size() == 1;
    super.setValid( valid );
    this.parentDatasource.setFinishable( finishable );
  }

  @Override
  public void stepActivatingForward() {
    super.stepActivatingForward();
    if ( this.joinGuiModel.getAvailableTables() == null || this.joinGuiModel.getAvailableTables().size() == 0 ) {
      fetchTables();
    }
    checkValidState();
  }

  public Domain getDomain() {
    return domain;
  }

  public void setDomain( Domain domain ) {
    this.domain = domain;
  }

  public MultiTableDatasourceDTO getDatasourceDTO() {
    return datasourceDTO;
  }

  public void setDatasourceDTO( MultiTableDatasourceDTO datasourceDTO ) {
    this.datasourceDTO = datasourceDTO;
  }

  public void closeWaitingDialog() {
    MessageHandler.getInstance().closeWaitingDialog();
  }

  public void showWaitingDialog() {
    MessageHandler.getInstance().showWaitingDialog( MessageHandler.getString( "multitable.FETCHING_TABLE_INFO" ) );
  }

  protected void fetchTables() {
    showWaitingDialog();
    IDatabaseConnection connection = ( (MultiTableDatasource) parentDatasource ).getConnection();
    processAvailableTables( connection, schemas.getValue() );
  }

  class SchemaSelection implements PropertyChangeListener {
    public void propertyChange( PropertyChangeEvent evt ) {
      if ( evt.getNewValue() instanceof String && TablesSelectionStep.this.activated ) {
        fetchTables();
      }
    }
  }

  @Override
  public void refresh() {
    IDatabaseConnection connection = ( (MultiTableDatasource) parentDatasource ).getConnection();
    joinSelectionServiceGwtImpl.retrieveSchemas( connection, new XulServiceCallback<List>() {
      public void error( String message, Throwable error ) {
        error.printStackTrace();
        mtdatasource.displayErrors( new JoinError( message, error.getMessage() ) );
      }

      public void success( List schemaValues ) {
        schemas.removePropertyChangeListener( schemaSelection );
        joinGuiModel.setSchemas( schemaValues );
        schemas.addPropertyChangeListener( schemaSelection );
        fetchTables();
      }
    } );
  }
}

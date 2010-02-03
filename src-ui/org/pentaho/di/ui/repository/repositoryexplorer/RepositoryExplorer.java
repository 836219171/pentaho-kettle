/*
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
 * Copyright (c) 2009 Pentaho Corporation.  All rights reserved.
 */
package org.pentaho.di.ui.repository.repositoryexplorer;

import java.util.Enumeration;
import java.util.ResourceBundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Composite;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.Directory;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.ui.repository.repositoryexplorer.controllers.BrowseController;
import org.pentaho.di.ui.repository.repositoryexplorer.controllers.ClustersController;
import org.pentaho.di.ui.repository.repositoryexplorer.controllers.ConnectionsController;
import org.pentaho.di.ui.repository.repositoryexplorer.controllers.MainController;
import org.pentaho.di.ui.repository.repositoryexplorer.controllers.PartitionsController;
import org.pentaho.di.ui.repository.repositoryexplorer.controllers.PermissionsController;
import org.pentaho.di.ui.repository.repositoryexplorer.controllers.SecurityController;
import org.pentaho.di.ui.repository.repositoryexplorer.controllers.SlavesController;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryDirectory;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.XulException;
import org.pentaho.ui.xul.XulRunner;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.binding.DefaultBindingFactory;
import org.pentaho.ui.xul.containers.XulDialog;
import org.pentaho.ui.xul.swt.SwtXulLoader;
import org.pentaho.ui.xul.swt.SwtXulRunner;

/**
 *
 */
public class RepositoryExplorer {

  private static Log log = LogFactory.getLog(RepositoryExplorer.class);

  @SuppressWarnings("unchecked")
  private static final Class CLZ = RepositoryExplorer.class;
  private MainController mainController = new MainController();

  private BrowseController browseController = new BrowseController();

  private SecurityController securityController = new SecurityController();

  private ConnectionsController connectionsController = new ConnectionsController();

  private SlavesController slavesController = new SlavesController();

  private PartitionsController partitionsController = new PartitionsController();

  private ClustersController clustersController = new ClustersController();

  private PermissionsController permissionsController = new PermissionsController();

  private XulDomContainer container;

  private Directory repositoryDirectory;
  
  private ResourceBundle resourceBundle = new ResourceBundle() {

    @Override
    public Enumeration<String> getKeys() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    protected Object handleGetObject(String key) {
      return BaseMessages.getString(CLZ, key);
    }
    
  };  

  // private Repository repository;

  public RepositoryExplorer(Directory rd, Repository rep, RepositoryExplorerCallback callback,
      VariableSpace variableSpace) {
    repositoryDirectory = rd;
    /// repository = rep;
    try {
      container = new SwtXulLoader().loadXul("org/pentaho/di/ui/repository/repositoryexplorer/xul/explorer-layout.xul", resourceBundle); //$NON-NLS-1$
      final XulRunner runner = new SwtXulRunner();
      runner.addContainer(container);

      BindingFactory bf = new DefaultBindingFactory();
      bf.setDocument(container.getDocumentRoot());

      mainController.setRepository(rep);
      mainController.setBindingFactory(bf);
      container.addEventHandler(mainController);

      boolean versionsEnabled = rep.getRepositoryMeta().getRepositoryCapabilities().supportsRevisions();
      loadVersionOverlay(versionsEnabled);
      browseController.setBindingFactory(bf);
      container.addEventHandler(browseController);
      browseController.setRepositoryDirectory(new UIRepositoryDirectory(repositoryDirectory, rep));
      browseController.setCallback(callback);

      permissionsController.setBindingFactory(bf);
      permissionsController.setBrowseController(browseController);
      permissionsController.setMessages(resourceBundle);
      container.addEventHandler(permissionsController);
      permissionsController.setRepositoryDirectory(new UIRepositoryDirectory(repositoryDirectory, rep));

      connectionsController.setRepository(rep);
      connectionsController.setMessages(resourceBundle);
      connectionsController.setBindingFactory(bf);
      container.addEventHandler(connectionsController);

      slavesController.setRepository(rep);
      slavesController.setBindingFactory(bf);
      slavesController.setMessages(resourceBundle);
      container.addEventHandler(slavesController);

      partitionsController.setRepository(rep);
      partitionsController.setVariableSpace(variableSpace);
      partitionsController.setBindingFactory(bf);
      partitionsController.setMessages(resourceBundle);
      container.addEventHandler(partitionsController);

      clustersController.setRepository(rep);
      clustersController.setBindingFactory(bf);
      clustersController.setMessages(resourceBundle);
      container.addEventHandler(clustersController);

      boolean securityEnabled = rep.getRepositoryMeta().getRepositoryCapabilities().managesUsers();
      loadSecurityOverlay(securityEnabled);

      boolean aclEnabled = rep.getRepositoryMeta().getRepositoryCapabilities().supportsAcls();
      loadAclOverlay(aclEnabled);

      container.addEventHandler(securityController);
      if (securityEnabled) {
        securityController.setBindingFactory(bf);
        securityController.setRepositoryUserInterface(rep.getSecurityProvider());
        securityController.setMessages(resourceBundle);
      }

      boolean aclsEnabled = rep.getRepositoryMeta().getRepositoryCapabilities().supportsAcls();
      container.addEventHandler(permissionsController);
      if (aclsEnabled && securityEnabled) {
        permissionsController.setBindingFactory(bf);
        permissionsController.setRepositoryUserInterface(rep.getSecurityProvider());
        permissionsController.setMessages(resourceBundle);
      }

      try {
        runner.initialize();
      } catch (XulException e) {
        log.error(resourceBundle.getString("RepositoryExplorer.ErrorStartingXulApplication"), e);//$NON-NLS-1$
      }

    } catch (XulException e) {
      log.error(resourceBundle.getString("RepositoryExplorer.ErrorLoadingXulApplication"), e);//$NON-NLS-1$
    }
  }

  public Composite getDialogArea() {
    XulDialog dialog = (XulDialog) container.getDocumentRoot().getElementById("repository-explorer-dialog"); //$NON-NLS-1$
    return (Composite) dialog.getManagedObject();
  }

  public void show() {
    XulDialog dialog = (XulDialog) container.getDocumentRoot().getElementById("repository-explorer-dialog"); //$NON-NLS-1$
    dialog.show();

  }

  private void loadSecurityOverlay(boolean securityEnabled) {
    String overlay = null;
    try {
      overlay = securityEnabled ? "org/pentaho/di/ui/repository/repositoryexplorer/xul/security-enabled-layout-overlay.xul" : //$NON-NLS-1$
          "org/pentaho/di/ui/repository/repositoryexplorer/xul/security-disabled-layout-overlay.xul"; //$NON-NLS-1$
      container.loadOverlay(overlay);
    } catch (XulException e) {
      log.error(BaseMessages.getString(CLZ, "RepositoryExplorer.ErrorLoadingXulOverlay", overlay));//$NON-NLS-1$
      e.printStackTrace();
    }
  }

  private void loadAclOverlay(boolean aclEnabled) {
    String overlay = null;
    try {
      overlay = aclEnabled ? "org/pentaho/di/ui/repository/repositoryexplorer/xul/acl-enabled-layout-overlay.xul" : //$NON-NLS-1$
          "org/pentaho/di/ui/repository/repositoryexplorer/xul/acl-disabled-layout-overlay.xul"; //$NON-NLS-1$
      container.loadOverlay(overlay);
    } catch (XulException e) {
      log.error(BaseMessages.getString(CLZ, "RepositoryExplorer.ErrorLoadingXulOverlay", overlay));//$NON-NLS-1$
      e.printStackTrace();
    }
  }

  private void loadVersionOverlay(boolean versionEnabled) {
    String overlay = null;
    try {
      overlay = versionEnabled ? "org/pentaho/di/ui/repository/repositoryexplorer/xul/version-layout-overlay.xul" : //$NON-NLS-1$
          "org/pentaho/di/ui/repository/repositoryexplorer/xul/version-disabled-layout-overlay.xul"; //$NON-NLS-1$
      container.loadOverlay(overlay);
    } catch (XulException e) {
      log.error(BaseMessages.getString(CLZ, "RepositoryExplorer.ErrorLoadingXulOverlay", overlay));//$NON-NLS-1$
      e.printStackTrace();
    }
  }

  public Directory getRepositoryDirectory() {
    return repositoryDirectory;
  }

  public void setRepositoryDirectory(Directory repositoryDirectory) {
    this.repositoryDirectory = repositoryDirectory;
  }

}

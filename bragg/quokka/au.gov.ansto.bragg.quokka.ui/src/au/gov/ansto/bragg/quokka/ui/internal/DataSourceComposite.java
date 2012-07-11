/*******************************************************************************
 * Copyright (c) 2007 Australian Nuclear Science and Technology Organisation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Based on work by Norman Xiong and Danil Klimontov
 *     Paul Hathaway customised for Quokka (10/62009)
 *******************************************************************************/
package au.gov.ansto.bragg.quokka.ui.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.gumtree.data.Factory;
import org.gumtree.data.interfaces.IDataset;
import org.gumtree.data.interfaces.IGroup;

import au.gov.ansto.bragg.datastructures.core.StaticDefinition.DataStructureType;
import au.gov.ansto.bragg.kakadu.core.DataListener;
import au.gov.ansto.bragg.kakadu.core.DataSourceManager;
import au.gov.ansto.bragg.kakadu.core.DataSourceManager.SelectableDataItem;
import au.gov.ansto.bragg.kakadu.core.data.DataItem;
import au.gov.ansto.bragg.kakadu.core.data.DataSourceFile;
import au.gov.ansto.bragg.kakadu.ui.Activator;
import au.gov.ansto.bragg.kakadu.ui.SWTResourceManager;
import au.gov.ansto.bragg.kakadu.ui.util.Util;
import au.gov.ansto.bragg.kakadu.ui.widget.CheckboxTableTreeViewer;
import au.gov.ansto.bragg.kakadu.ui.widget.tree.DefaultTreeContentProvider;

/**
 * The class displays and manages data source selection.
 */
public class DataSourceComposite extends Composite {
	private Composite parentComposite;

	private CheckboxTableTreeViewer fileTableTreeViewer;
	private DefaultMutableTreeNode rootNode;
	private DataSourceTableLabelProvider dataSourceTableLabelProvider;
	private DataSourceViewerSorter dataSourceViewerSorter;
	private Listener sortSelectionListener;
	private DropTarget dropTarget;

	private Action addFileAction;
	private Action addDirectoryAction;
	private Action combineSelectedFilesAction;
	private Action removeFileAction;
	private Action removeAllAction;
	private Action deselectAllAction;
	private Action selectAllAction;

	private IWorkbenchAction dynamicHelpAction;

	public DataSourceComposite(org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, style);
		SWTResourceManager.registerResourceUser(parent);
		this.parentComposite = parent;
		initialise();
		initListeners();
		createActions();
	}

	private void initialise() {
		GridLayout thisLayout = new GridLayout();
		thisLayout.marginHeight = 0;
		thisLayout.horizontalSpacing = 3;
		thisLayout.marginWidth = 0;
		thisLayout.verticalSpacing = 3;
		thisLayout.numColumns = 1;
		this.setLayout(thisLayout);
		
		//init table tree
		fileTableTreeViewer = new CheckboxTableTreeViewer(this, SWT.FULL_SELECTION | SWT.CHECK);
		
		GridData fileTreeViewerLData = new GridData();
		fileTreeViewerLData.horizontalAlignment = GridData.FILL;
		fileTreeViewerLData.verticalAlignment = GridData.FILL;
		fileTreeViewerLData.grabExcessHorizontalSpace = true;
		fileTreeViewerLData.grabExcessVerticalSpace = true;
		fileTreeViewerLData.horizontalSpan = 2;
		fileTableTreeViewer.getControl().setLayoutData(fileTreeViewerLData);

		fileTableTreeViewer.setContentProvider(new DefaultTreeContentProvider());
		
		dataSourceTableLabelProvider = new DataSourceTableLabelProvider();
		fileTableTreeViewer.setLabelProvider(dataSourceTableLabelProvider);

		dataSourceViewerSorter = new DataSourceViewerSorter();
		fileTableTreeViewer.setSorter(dataSourceViewerSorter);
		
		fileTableTreeViewer.setInput(getRootNode()); // pass a non-null that will be ignored
		fileTableTreeViewer.setAutoCheckedMode(true);

		final Table table = fileTableTreeViewer.getTableTree().getTable();
		table.setHeaderVisible(true);
		
		TableColumn nameColumn = new TableColumn(table, SWT.None);
		nameColumn.setText("Name");
		nameColumn.setWidth(200);
		nameColumn.setMoveable(true);
		dataSourceTableLabelProvider.addTableColumn(nameColumn);
		
		sortSelectionListener = new SortSelectionListener();
		nameColumn.addListener(SWT.Selection, sortSelectionListener);

		this.layout();
		createDropTarget();
		initData();
	}
	
	private void initData() {
		//initialise by existed files in DataSourceManager
		//the case will work when View has been closed and then opened again
		for (Iterator<DataSourceFile> filesIterator = DataSourceManager.getInstance().getFiles(); filesIterator.hasNext();) {
			DataSourceFile file = filesIterator.next();
			
			//create DataItem nodes
			DefaultMutableTreeNode fileNode = createNode(file);
			
			//add nodes to view
			addFileNode(fileNode);
			
			//update expanded state
			fileTableTreeViewer.setExpandedState(fileNode, true);
			
			//update checked state
			for (Enumeration e = fileNode.children(); e.hasMoreElements();) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
				final Object userObject = childNode.getUserObject();
				if (userObject instanceof SelectableDataItem) {
					SelectableDataItem selectableDataItem = (SelectableDataItem) userObject;
					if (selectableDataItem.isSelected()) {
						fileTableTreeViewer.setChecked(childNode, true);
					}
				}
			}
		}
	}

	/**
	 * Adds not existed columns to table.
	 * @param columnNames
	 */
	private void updateTableColumns(List<String> columnNames) {
		for (String columnName : columnNames) {
			if (!dataSourceTableLabelProvider.isColumnExist(columnName)) {
				final Table table = fileTableTreeViewer.getTableTree().getTable();
				TableColumn tableColumn = new TableColumn(table, SWT.None);
				tableColumn.setText(columnName);
				tableColumn.setWidth(200);
				tableColumn.setMoveable(true);
				tableColumn.addListener(SWT.Selection, sortSelectionListener);
				dataSourceTableLabelProvider.addTableColumn(tableColumn);
			}
		}
		
	}

	private DefaultMutableTreeNode getRootNode() {
		if (rootNode == null) {
			rootNode = new DefaultMutableTreeNode();
		}		
		return rootNode;
	}
	
	/**
	 * Adds data files from the directory to the list of source files.
	 * @param directoryName path to the directory with data files.
	 */
	public void addDirectory(String directoryName) {
		File directory = new File(directoryName);
		if (directory.isDirectory()) {
			File[] directoryFiles = directory.listFiles(new FileFilter() {
				public boolean accept(File file) {
					//filter for only known formats
					String name = file.getName();
					int lastDotPosition = name.lastIndexOf(".");
					String extention = name.substring(lastDotPosition + 1, name.length()).toLowerCase();
					return Util.getSupportedFileExtentions().contains(extention);
				}			
			});
			
			for (int i = 0; i < directoryFiles.length - 1; i ++)
				addFile(directoryFiles[i].getAbsolutePath(), false);
			
			File lastFile = directoryFiles[directoryFiles.length - 1];
			addFile(lastFile.getAbsolutePath());

			adjustColumnSize();
		} else {
			new IllegalArgumentException("The path '" +directoryName+"' is not a directory.");
		}
	}
	
	public DataSourceFile addFile(String filePath) {
		return addFile(filePath, true);
	}
	/**
	 * Adds data file to the list of source files. 
	 * @param filePath absolute file path to the file.
	 */
	public DataSourceFile addFile(String filePath, boolean willInformListener) {
		
		//check duplications
		DataSourceFile dataSourceFile = DataSourceManager.getInstance().getDataSourceFile(filePath);
		if (dataSourceFile != null) {
			if (confirmFileReplace(filePath)) {
				DataSourceManager.getInstance().removeFile(dataSourceFile);

				//update UI
				removeFileNode(dataSourceFile);
			} else {
				return dataSourceFile;
			}
		}
		
		//add file to DataSourceManager
		try {
			dataSourceFile = DataSourceManager.getInstance().addFile(filePath);
		} catch (IOException e) {
			Util.handleException(getShell(), e);
			return null;
		} catch (Exception e) {
			Util.handleException(getShell(), e);
			return null;
		}

		//create DataItem nodes
		DefaultMutableTreeNode fileNode = createNode(dataSourceFile);
		
		//add nodes to view
		addFileNode(fileNode);
		for (TableTreeItem item : fileTableTreeViewer.getTableTree().getItems()){
			if (item.getData() == fileNode)
				fileTableTreeViewer.getTableTree().setSelection(new TableTreeItem[]{item});		
		}

		//in this version of view, immediately set the data source to allow immediate
		// triggering of algorithm
		if (willInformListener){
			DataSourceManager.setSelectedFile(dataSourceFile);
			removeFileAction.setEnabled(fileTableTreeViewer.getTableTree().getSelection().length > 0);
		}

		//update expanded/checked state
		fileTableTreeViewer.setExpandedState(fileNode, false);
		fileTableTreeViewer.setSubtreeChecked(fileNode, true);
		return dataSourceFile;
	}

	/**
	 * @param dataSourceFile
	 * @return
	 */
	private DefaultMutableTreeNode createNode(DataSourceFile dataSourceFile) {
		DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(dataSourceFile);
		return fileNode;
	}

	private void adjustColumnSize() {
		//adjust column size
		for (TableColumn tableColumn : fileTableTreeViewer.getTableTree().getTable().getColumns()) {
			tableColumn.pack();
		}
	}

	private void addFileNode(DefaultMutableTreeNode fileNode) {
		fileTableTreeViewer.add(getRootNode(), fileNode);
		getRootNode().add(fileNode);
	}

	private void removeFileNode(DataSourceFile dataSourceFile) {
		DefaultMutableTreeNode fileNode = getFileNode(dataSourceFile);
		removeFileNode(fileNode);
	}
	
	private void removeFileNode(DefaultMutableTreeNode fileNode) {
		getRootNode().remove(fileNode);
		fileTableTreeViewer.remove(fileNode);
	}

	private void removeAllNodes() {
		List childrenNodes = new ArrayList();
		for (Enumeration children = getRootNode().children(); children.hasMoreElements();) {
			childrenNodes.add(children.nextElement());
		}
		getRootNode().removeAllChildren();
		fileTableTreeViewer.remove(childrenNodes.toArray());
	}


	private DefaultMutableTreeNode getFileNode(DataSourceFile dataSourceFile) {
		for (Enumeration children = getRootNode().children();children.hasMoreElements();) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
			if (node.getUserObject() == dataSourceFile) {
				return node;
			}
		}
		return null;
	}

	private boolean confirmFileReplace(String fileName) {
		return MessageDialog.openQuestion(
				getShell(),
				"Data Source View - Add data file",
				"The file '" + fileName + "' already exists.\nDo you want to replace and reload it?");
	}

	private boolean confirmFileRemove(String fileName) {
		return MessageDialog.openQuestion(
				getShell(),
				"Data Source View - Remove data file",
				"Are you sure you want to remove file '" + fileName + "'?");
	}

	private boolean confirmRemoveAll() {
		return MessageDialog.openQuestion(
				getShell(),
				"Data Source View - Remove all",
				"Are you sure you want to remove all files from the list?");
	}

	private void initListeners() {
		fileTableTreeViewer.addCheckStateListener(new ICheckStateListener() {
	        public void checkStateChanged(CheckStateChangedEvent event) {
	        	//transfer events from view to model
	        	Object element = event.getElement();
	        	if (element instanceof DefaultMutableTreeNode) {
					DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) element;
					Object userObject = treeNode.getUserObject();
					if (userObject instanceof SelectableDataItem) {
						SelectableDataItem selectableDataItem = (SelectableDataItem) userObject;
						selectableDataItem.setSelected(event.getChecked());
					}
				}
	        }
	    });
		fileTableTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				removeFileAction.setEnabled(!event.getSelection().isEmpty());
//				toolItemRemoveFile.setEnabled(!event.getSelection().isEmpty());
			}
		});
		

		DataSourceManager.getInstance().addDataListener(new DataListener<DataSourceFile>() {
			public void dataAdded(DataSourceFile addedData) {
				updateButtons();
			}
			public void dataRemoved(DataSourceFile removedData) {
				updateButtons();
			}
			public void dataUpdated(DataSourceFile updatedData) {
				updateButtons();
			}
			public void allDataRemoved(DataSourceFile removedData) {
				updateButtons();
			}
		});
	}
	/**
	 * Update button state. 
	 */
	private void updateButtons() {
		boolean dataExiests = DataSourceManager.getInstance().getSourceDataFileCount() > 0;
		removeAllAction.setEnabled(dataExiests);
		selectAllAction.setEnabled(dataExiests);
		deselectAllAction.setEnabled(dataExiests);		
	}
	
	protected void createActions() {
		addFileAction = new Action() {
			public void run() {
				String[] selectedFiles = Util.selectFilesFromShell(
						parentComposite.getShell(), "*.hdf", "HDF data file");
				for (int i = 0; i < selectedFiles.length - 1; i++) {
					addFile(selectedFiles[i], false);
				}
				addFile(selectedFiles[selectedFiles.length - 1]);
				adjustColumnSize();
			}
		};
		addFileAction.setText("Add File(s)");
		addFileAction.setToolTipText("Add data file(s) to view");
		addFileAction.setImageDescriptor(Activator.getImageDescriptor("icons/add_item.gif"));
		addFileAction.setDisabledImageDescriptor(Activator.getImageDescriptor("icons/add_item_dis.gif"));

		addDirectoryAction = new Action() {
			public void run() {
				String selectedDirectory = Util.selectDirectoryFromShell(parentComposite.getShell());
				addDirectory(selectedDirectory);
			}
		};
		addDirectoryAction.setText("Add Directory");
		addDirectoryAction.setToolTipText("Add data files from a directory to view");
		addDirectoryAction.setImageDescriptor(Activator.getImageDescriptor("icons/add_dir.gif"));
		addDirectoryAction.setDisabledImageDescriptor(Activator.getImageDescriptor("icons/add_dir_dis.gif"));
		
		combineSelectedFilesAction = new Action() {
			public void run() {
				try {
					combineSelectedFiles();
				}catch (Exception e) {
					Util.handleException(getShell(), e);
				}
			}
		};
		combineSelectedFilesAction.setText("Combine Data");
		combineSelectedFilesAction.setToolTipText("Combine selected data to a group");
		combineSelectedFilesAction.setImageDescriptor(Activator.getImageDescriptor("icons/combine_data.gif"));
		combineSelectedFilesAction.setDisabledImageDescriptor(Activator.getImageDescriptor("icons/combine_data_dis.gif"));
		combineSelectedFilesAction.setEnabled(false);
		
		removeFileAction = new Action() {
			public void run() {
				removeSelectedFile();
			}
		};
		removeFileAction.setText("Remove File");
		removeFileAction.setToolTipText("Remove selected data file from view");
		removeFileAction.setImageDescriptor(Activator.getImageDescriptor("icons/rem_item.gif"));
		removeFileAction.setDisabledImageDescriptor(Activator.getImageDescriptor("icons/rem_item_dis.gif"));
		removeFileAction.setEnabled(false);
		

		removeAllAction = new Action() {
			public void run() {
				removeAll();
				removeAllAction.setEnabled(false);
			}
		};
		removeAllAction.setText("Remove All");
		removeAllAction.setToolTipText("Remove All data file(s) from view");
		removeAllAction.setImageDescriptor(Activator.getImageDescriptor("icons/rem_all_items.gif"));
		removeAllAction.setDisabledImageDescriptor(Activator.getImageDescriptor("icons/rem_all_items_dis.gif"));


		selectAllAction = new Action() {
			public void run() {
				fileTableTreeViewer.setAllChecked(true);
			}
		};
		selectAllAction.setText("Select All");
		selectAllAction.setToolTipText("Select all data items");
		selectAllAction.setImageDescriptor(Activator.getImageDescriptor("icons/select_all.gif"));
//		selectAllAction.setDisabledImageDescriptor(Activator.getImageDescriptor("icons/add_item_dis.gif"));
		
		deselectAllAction = new Action() {
			public void run() {
				fileTableTreeViewer.setAllChecked(false);
			}
		};
		deselectAllAction.setText("Deselect All");
		deselectAllAction.setToolTipText("Deselect all data items");
		deselectAllAction.setImageDescriptor(Activator.getImageDescriptor("icons/deselect_all.gif"));
//		deselectAllAction.setDisabledImageDescriptor(Activator.getImageDescriptor("icons/add_item_dis.gif"));
		
		dynamicHelpAction = ActionFactory.DYNAMIC_HELP.create(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		dynamicHelpAction.setImageDescriptor(ActionFactory.HELP_CONTENTS.create(PlatformUI.getWorkbench().getActiveWorkbenchWindow()).getImageDescriptor());

		updateButtons();
	}

	/**
	 * Gets all actions for the composite.
	 * The method can be used to contribute to view bar/menu actions. 
	 * @return
	 */
	public List getActionList() {
		final ArrayList result = new ArrayList();
		result.add(addFileAction);
		result.add(addDirectoryAction);
		result.add(combineSelectedFilesAction);
		result.add(removeFileAction);
		result.add(removeAllAction);
		result.add(new Separator());
		result.add(selectAllAction);
		result.add(deselectAllAction);
		result.add(dynamicHelpAction);
		
		return result;
	}

	
	private void createDropTarget() {
		if (dropTarget != null) 
			dropTarget.dispose();
		int dropOperation = DND.DROP_COPY;
		final int dropDefaultOperation = DND.DROP_COPY;
		final int dropFeedback = DND.FEEDBACK_INSERT_AFTER | DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL | DND.FEEDBACK_SELECT;
		
		dropTarget = new DropTarget(fileTableTreeViewer.getTableTree(), dropOperation);
		Transfer[] dropTypes = new Transfer[] {TextTransfer.getInstance(), FileTransfer.getInstance()};
		dropTarget.setTransfer(dropTypes);
		dropTarget.addDropListener(new DropTargetListener() {
			public void dragEnter(DropTargetEvent event) {
//				System.out.println(">>dragEnter\n");
				if (event.detail == DND.DROP_DEFAULT) {
					event.detail = dropDefaultOperation;
				} 
				if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
					event.detail = DND.DROP_COPY;
				}
				event.feedback = dropFeedback;
			}
			public void dragLeave(DropTargetEvent event) {
//				System.out.println(">>dragLeave\n");
			}
			public void dragOperationChanged(DropTargetEvent event) {
//				System.out.println(">>dragOperationChanged\n");
				if (event.detail == DND.DROP_DEFAULT) {
					event.detail = dropDefaultOperation;
				}
				event.feedback = dropFeedback;
			}
			public void dragOver(DropTargetEvent event) {
//				System.out.println(">>dragOver\n");
				event.feedback = dropFeedback;
			}
			public void drop(DropTargetEvent event) {
//				System.out.println(">>drop\n");
				String[] strings = null;
//				if (TextTransfer.getInstance().isSupportedType(event.currentDataType) ||
//				    RTFTransfer.getInstance().isSupportedType(event.currentDataType) ||
//				    HTMLTransfer.getInstance().isSupportedType(event.currentDataType)) {
//				    strings = new String[] {(String)event.data};
//				}
				if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
					strings = (String[])event.data;
				}
				if (strings == null || strings.length == 0) {
					System.out.println("!!Invalid data dropped");
					return;
				}
				
				//load droped files
				for (String fileName : strings) {
					File file = new File(fileName);
					if (file.isDirectory()) {
						addDirectory(fileName);
					} else {
						addFile(fileName);
					}
				} 
				
			}
			public void dropAccept(DropTargetEvent event) {
//				System.out.println(">>dropAccept\n");
			}
		});
	}

	protected void combineSelectedFiles() throws Exception{
		IDataset dataset = Factory.createEmptyDatasetInstance();
		IGroup rootGroup = dataset.getRootGroup();
		IGroup newEntry = Factory.createGroup(rootGroup, "CombinedEntry", true);
		au.gov.ansto.bragg.datastructures.core.Util.setDataStructure(newEntry, DataStructureType.combined);
//		Dataset dataset = null;
//		Group newEntry = null;
//		TableTreeItem[] selection = fileTableTreeViewer.getTableTree().getSelection();
		Object[] selection = fileTableTreeViewer.getCheckedElements();
//		TableTreeItem[] selection = null;
		for (Object itemData : selection) {
//			Object itemData = item.getData();
			if (itemData instanceof DefaultMutableTreeNode) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) itemData;
				Object userObject = treeNode.getUserObject();
				
				DefaultMutableTreeNode treeNodeToRemove = null;
				DataSourceFile dataSourceFile = null;
				
				if (userObject instanceof DataItem) {
//					DataItem dataSet = (DataItem) userObject;
					treeNodeToRemove = (DefaultMutableTreeNode) treeNode.getParent();
					dataSourceFile = (DataSourceFile) treeNodeToRemove.getUserObject();
//					if (newEntry == null){
//						dataset = ((DataItem)userObject).getDataObject().getDataset();
//						newEntry = Factory.createGroup(dataset.getRootGroup(), "CombinedEntry", true);
//					}
					if (rootGroup.findDictionary() == null || rootGroup.findDictionary().getAllKeys().size() <= 1)
						rootGroup.setDictionary(((DataItem)userObject).getDataObject().getRootGroup().findDictionary());
					newEntry.addSubgroup(((DataItem)userObject).getDataObject());
				} else if (userObject instanceof DataSourceFile) {
					dataSourceFile = (DataSourceFile) userObject;
					treeNodeToRemove = treeNode;
				}
				//confirm file removing
				if (treeNodeToRemove != null &&
						dataSourceFile != null ) {
					DataSourceManager.getInstance().removeFile(dataSourceFile);
					try{
					removeFileNode(treeNodeToRemove);
					}catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}
				}
			}
		}
		DataSourceFile dataSourceFile = null;
		try {
			dataSourceFile = DataSourceManager.getInstance().addDataset(dataset);
		} catch (IOException e) {
			Util.handleException(getShell(), e);
		} catch (Exception e) {
			Util.handleException(getShell(), e);
		}

		//create DataItem nodes
		DefaultMutableTreeNode fileNode = createNode(dataSourceFile);
		
		//add nodes to view
		addFileNode(fileNode);

		//update expanded/checked state
		fileTableTreeViewer.setExpandedState(fileNode, true);
		fileTableTreeViewer.setSubtreeChecked(fileNode, true);
	}

	private TableTreeItem[] getSelections() {
		// TODO Auto-generated method stub
		return null;
	}

	protected void removeSelectedFile() {
		TableTreeItem[] selection = fileTableTreeViewer.getTableTree().getSelection();
		for (TableTreeItem item : selection) {
			int selectionIndex = 0;
			for (TableTreeItem tableItem : fileTableTreeViewer.getTableTree().getItems()){
				if (tableItem == item)
					break;
				selectionIndex ++;
			}
			Object itemData = item.getData();
			if (itemData instanceof DefaultMutableTreeNode) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) itemData;
				Object userObject = treeNode.getUserObject();
				
				DefaultMutableTreeNode treeNodeToRemove = null;
				DataSourceFile dataSourceFile = null;
				
				if (userObject instanceof DataItem) {
//					DataItem dataSet = (DataItem) userObject;
					treeNodeToRemove = (DefaultMutableTreeNode) treeNode.getParent();
					dataSourceFile = (DataSourceFile) treeNodeToRemove.getUserObject();
					
				} else if (userObject instanceof DataSourceFile) {
					dataSourceFile = (DataSourceFile) userObject;
					treeNodeToRemove = treeNode;
				}
				//confirm file removing
				if (treeNodeToRemove != null &&
						dataSourceFile != null &&
						confirmFileRemove(dataSourceFile.getName())) {
					DataSourceManager.getInstance().removeFile(dataSourceFile);
					removeFileNode(treeNodeToRemove);
				}
			}
			int size = fileTableTreeViewer.getTableTree().getItemCount();
			if (selectionIndex >= size)
				selectionIndex = size - 1;
			if (selectionIndex >= 0){
				TableTreeItem newFocusItem = fileTableTreeViewer.getTableTree()
					.getItem(selectionIndex);				
				fileTableTreeViewer.getTableTree().setSelection(new TableTreeItem[]{
					newFocusItem});
				Object newFocusData = ((DefaultMutableTreeNode) newFocusItem.getData()
						).getUserObject();
				if (newFocusData instanceof DataSourceFile)
					DataSourceManager.setSelectedFile((DataSourceFile) newFocusData);
					removeFileAction.setEnabled(fileTableTreeViewer.getTableTree().getSelection().length > 0);
			} else
				DataSourceManager.fireAllDataRemovedAction();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		removeAll();
		super.dispose();
	}

	public void removeAll() {
		if (fileTableTreeViewer.getTableTree().getItemCount() > 0 &&
				confirmRemoveAll()) {
			DataSourceManager.getInstance().removeAll();
			removeAllNodes();
			DataSourceManager.fireAllDataRemovedAction();
		}
	}

	private final class SortSelectionListener implements Listener {
		public void handleEvent(Event event) {
			//store checked nodes for check state restoring
			Object[] checkedElements = fileTableTreeViewer.getCheckedElements();
			
			//apply sorting
			dataSourceViewerSorter.setSortColumn((TableColumn)event.widget);
			fileTableTreeViewer.refresh(false);
			
			//clear checked sate
			fileTableTreeViewer.setAllChecked(false);
			
			//compose list of only DataItem nodes
			ArrayList<DefaultMutableTreeNode> checkedDataNodes = new ArrayList<DefaultMutableTreeNode>();
			for (int i = 0; i < checkedElements.length; i++) {
				Object element = checkedElements[i];
				if (element instanceof DefaultMutableTreeNode) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) element;
					Object userObject = node.getUserObject();
					if (userObject instanceof DataItem) {
						checkedDataNodes.add(node);
					}
				}
				
			}
			
			//update checked state for all DataItem naodes.
			//All file nodes will be updated automaticaly
			fileTableTreeViewer.setCheckedElements(checkedDataNodes.toArray());
		}
	}

	public class DataSourceViewerSorter extends ViewerSorter {

		private TableColumn column;

		private int direction = SWT.NONE;

		/**
		 * Does the sort. If it's a different column from the previous sort, do an
		 * ascending sort. If it's the same column as the last sort, toggle the sort
		 * direction.
		 *
		 * @param column
		 */
		public void setSortColumn(TableColumn column) {
			final Table table = fileTableTreeViewer.getTableTree().getTable();
			if (column == this.column) {
				// Same column as last sort; toggle the direction
				switch (direction) {
				case SWT.DOWN:
					direction = SWT.UP;
					break;
//				case SWT.UP:
//					direction = SWT.NONE;
//					break;

				default:
					direction = SWT.DOWN;
					break;
				}

			} else {
				// New column; do an ascending sort
				this.column = column;
				direction = SWT.DOWN;

				table.setSortColumn(column);
			}
			
			table.setSortDirection(direction);
		}

		/**
		 * Compares the object for sorting
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (direction == SWT.NONE) {
				//not sorted result
				return 0;
			}
	        String name1;
	        String name2;

	        if (viewer == null || !(viewer instanceof ContentViewer)) {
	            name1 = e1.toString();
	            name2 = e2.toString();
	        } else {
	            IBaseLabelProvider prov = ((ContentViewer) viewer)
	                    .getLabelProvider();
	            if (prov instanceof ITableLabelProvider) {
	            	ITableLabelProvider lprov = (ITableLabelProvider) prov;
	                int columnIndex = getColumnIndex(column);
	                
					name1 = lprov.getColumnText(e1, columnIndex);
	                name2 = lprov.getColumnText(e2, columnIndex);
	            } else {
	                name1 = e1.toString();
	                name2 = e2.toString();
	            }
	        }
	        if (name1 == null) {
				name1 = "";//$NON-NLS-1$
			}
	        if (name2 == null) {
				name2 = "";//$NON-NLS-1$
			}

	        // use the comparator to compare the strings
	        int result = getComparator().compare(name1, name2);
	        
	        if (direction == SWT.UP) {
	        	//opposite direction
	        	result = -result;
	        }
	        return result;
		}
	}

	public int getColumnIndex(TableColumn column) {
		return dataSourceTableLabelProvider.getColumnIndex(column);
	}

	public void setSelectionAll(boolean flag){
		fileTableTreeViewer.setAllChecked(flag);
	}

	public void selectDataSourceItem(URI fileUri, String entryName) {
		// TODO Auto-generated method stub
	}
	
	public void addSelectionChangedListener(SelectionListener listener){
		fileTableTreeViewer.addSelectionChangedListener(listener);
	}
	
	public void removeSelectionChangedListener(SelectionListener listener){
		fileTableTreeViewer.removeSelectionChangedListener(listener);
	}
	
}

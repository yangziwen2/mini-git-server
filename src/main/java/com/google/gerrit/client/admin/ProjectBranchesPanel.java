// Copyright 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.client.admin;

import com.google.gerrit.client.data.GitwebLink;
import com.google.gerrit.client.reviewdb.Branch;
import com.google.gerrit.client.reviewdb.Project;
import com.google.gerrit.client.rpc.Common;
import com.google.gerrit.client.rpc.GerritCallback;
import com.google.gerrit.client.rpc.InvalidNameException;
import com.google.gerrit.client.rpc.InvalidRevisionException;
import com.google.gerrit.client.ui.FancyFlexTable;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListenerAdapter;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwtjsonrpc.client.RemoteJsonException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProjectBranchesPanel extends Composite {
  private Project.Id projectId;

  private BranchesTable branches;
  private Button delBranch;
  private Button addBranch;
  private TextBox nameTxtBox;
  private TextBox irevTxtBox;

  public ProjectBranchesPanel(final Project.Id toShow) {
    final FlowPanel body = new FlowPanel();
    initBranches(body);
    initWidget(body);

    projectId = toShow;
  }

  @Override
  public void onLoad() {
    enableForm(false);
    super.onLoad();

    Util.PROJECT_SVC.listBranches(projectId,
        new GerritCallback<List<Branch>>() {
          public void onSuccess(final List<Branch> result) {
            enableForm(true);
            branches.display(result);
            branches.finishDisplay(true);
          }
        });
  }

  private void enableForm(final boolean on) {
    delBranch.setEnabled(on);
    addBranch.setEnabled(on);
    nameTxtBox.setEnabled(on);
    irevTxtBox.setEnabled(on);
  }

  private void initBranches(final Panel body) {
    final FlowPanel addPanel = new FlowPanel();
    addPanel.setStyleName("gerrit-AddSshKeyPanel");

    final Grid addGrid = new Grid(2, 2);

    nameTxtBox = new TextBox();
    nameTxtBox.setVisibleLength(50);
    nameTxtBox.setText(Util.C.defaultBranchName());
    nameTxtBox.addStyleName("gerrit-InputFieldTypeHint");
    nameTxtBox.addFocusListener(new FocusListenerAdapter() {
      @Override
      public void onFocus(Widget sender) {
        if (Util.C.defaultBranchName().equals(nameTxtBox.getText())) {
          nameTxtBox.setText("");
          nameTxtBox.removeStyleName("gerrit-InputFieldTypeHint");
        }
      }

      @Override
      public void onLostFocus(Widget sender) {
        if ("".equals(nameTxtBox.getText())) {
          nameTxtBox.setText(Util.C.defaultBranchName());
          nameTxtBox.addStyleName("gerrit-InputFieldTypeHint");
        }
      }
    });
    addGrid.setText(0, 0, Util.C.columnBranchName() + ":");
    addGrid.setWidget(0, 1, nameTxtBox);

    irevTxtBox = new TextBox();
    irevTxtBox.setVisibleLength(50);
    irevTxtBox.setText(Util.C.defaultRevisionSpec());
    irevTxtBox.addStyleName("gerrit-InputFieldTypeHint");
    irevTxtBox.addFocusListener(new FocusListenerAdapter() {
      @Override
      public void onFocus(Widget sender) {
        if (Util.C.defaultRevisionSpec().equals(irevTxtBox.getText())) {
          irevTxtBox.setText("");
          irevTxtBox.removeStyleName("gerrit-InputFieldTypeHint");
        }
      }

      @Override
      public void onLostFocus(Widget sender) {
        if ("".equals(irevTxtBox.getText())) {
          irevTxtBox.setText(Util.C.defaultRevisionSpec());
          irevTxtBox.addStyleName("gerrit-InputFieldTypeHint");
        }
      }
    });
    addGrid.setText(1, 0, Util.C.initialRevision() + ":");
    addGrid.setWidget(1, 1, irevTxtBox);

    addBranch = new Button(Util.C.buttonAddBranch());
    addBranch.addClickListener(new ClickListener() {
      public void onClick(final Widget sender) {
        doAddNewBranch();
      }
    });
    addPanel.add(addGrid);
    addPanel.add(addBranch);

    branches = new BranchesTable();

    delBranch = new Button(Util.C.buttonDeleteBranch());
    delBranch.addClickListener(new ClickListener() {
      public void onClick(final Widget sender) {
        branches.deleteChecked();
      }
    });

    body.add(branches);
    body.add(delBranch);
    body.add(addPanel);
  }

  private void doAddNewBranch() {
    String branchName = nameTxtBox.getText();
    if ("".equals(branchName) || Util.C.defaultBranchName().equals(branchName)) {
      return;
    }

    String rev = irevTxtBox.getText();
    if ("".equals(rev) || Util.C.defaultRevisionSpec().equals(rev)) {
      return;
    }

    if (!branchName.startsWith(Branch.R_REFS)) {
      branchName = Branch.R_HEADS + branchName;
    }

    addBranch.setEnabled(false);
    Util.PROJECT_SVC.addBranch(projectId, branchName, rev,
        new GerritCallback<List<Branch>>() {
          public void onSuccess(final List<Branch> result) {
            addBranch.setEnabled(true);
            nameTxtBox.setText("");
            irevTxtBox.setText("");
            branches.display(result);
          }

          @Override
          public void onFailure(final Throwable caught) {
            if (caught instanceof InvalidNameException
                || caught instanceof RemoteJsonException
                && caught.getMessage().equals(InvalidNameException.MESSAGE)) {
              nameTxtBox.selectAll();
              nameTxtBox.setFocus(true);

            } else if (caught instanceof InvalidRevisionException
                || caught instanceof RemoteJsonException
                && caught.getMessage().equals(InvalidRevisionException.MESSAGE)) {
              irevTxtBox.selectAll();
              irevTxtBox.setFocus(true);
            }

            addBranch.setEnabled(true);
            super.onFailure(caught);
          }
        });
  }

  private class BranchesTable extends FancyFlexTable<Branch> {
    BranchesTable() {
      table.setText(0, 2, Util.C.columnBranchName());
      table.setHTML(0, 3, "&nbsp;");
      table.addTableListener(new TableListener() {
        public void onCellClicked(SourcesTableEvents sender, int row, int cell) {
          if (cell != 1 && getRowItem(row) != null) {
            movePointerTo(row);
          }
        }
      });

      final FlexCellFormatter fmt = table.getFlexCellFormatter();
      fmt.addStyleName(0, 1, S_ICON_HEADER);
      fmt.addStyleName(0, 2, S_DATA_HEADER);
      fmt.addStyleName(0, 3, S_DATA_HEADER);
    }

    @Override
    protected Object getRowItemKey(final Branch item) {
      return item.getId();
    }

    @Override
    protected boolean onKeyPress(final char keyCode, final int modifiers) {
      if (super.onKeyPress(keyCode, modifiers)) {
        return true;
      }
      if (modifiers == 0) {
        switch (keyCode) {
          case 's':
          case 'c':
            toggleCurrentRow();
            return true;
        }
      }
      return false;
    }

    @Override
    protected void onOpenItem(final Branch item) {
      toggleCurrentRow();
    }

    private void toggleCurrentRow() {
      final CheckBox cb = (CheckBox) table.getWidget(getCurrentRow(), 1);
      cb.setChecked(!cb.isChecked());
    }

    void deleteChecked() {
      final HashSet<Branch.NameKey> ids = new HashSet<Branch.NameKey>();
      for (int row = 1; row < table.getRowCount(); row++) {
        final Branch k = getRowItem(row);
        if (k != null && table.getWidget(row, 1) instanceof CheckBox
            && ((CheckBox) table.getWidget(row, 1)).isChecked()) {
          ids.add(k.getNameKey());
        }
      }
      if (ids.isEmpty()) {
        return;
      }

      Util.PROJECT_SVC.deleteBranch(ids,
          new GerritCallback<Set<Branch.NameKey>>() {
            public void onSuccess(final Set<Branch.NameKey> deleted) {
              for (int row = 1; row < table.getRowCount();) {
                final Branch k = getRowItem(row);
                if (k != null && deleted.contains(k.getNameKey())) {
                  table.removeRow(row);
                } else {
                  row++;
                }
              }
            }
          });
    }

    void display(final List<Branch> result) {
      while (1 < table.getRowCount())
        table.removeRow(table.getRowCount() - 1);

      for (final Branch k : result) {
        final int row = table.getRowCount();
        table.insertRow(row);
        applyDataRowStyle(row);
        populate(row, k);
      }
    }

    void populate(final int row, final Branch k) {
      final GitwebLink c = Common.getGerritConfig().getGitwebLink();

      table.setWidget(row, 1, new CheckBox());
      table.setText(row, 2, k.getShortName());
      if (c != null) {
        table.setWidget(row, 3, new Anchor("(gitweb)", false, c.toBranch(k
            .getNameKey())));
      } else {
        table.setHTML(row, 3, "&nbsp;");
      }

      final FlexCellFormatter fmt = table.getFlexCellFormatter();
      fmt.addStyleName(row, 1, S_ICON_CELL);
      fmt.addStyleName(row, 2, S_DATA_CELL);
      fmt.addStyleName(row, 3, S_DATA_CELL);

      setRowItem(row, k);
    }
  }
}
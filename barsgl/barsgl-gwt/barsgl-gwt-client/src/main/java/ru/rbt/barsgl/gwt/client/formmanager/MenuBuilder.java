package ru.rbt.barsgl.gwt.client.formmanager;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import ru.rbt.barsgl.gwt.client.BarsGLEntryPoint;
import ru.rbt.barsgl.gwt.client.about.AboutForm;
import ru.rbt.barsgl.gwt.client.account.AccountForm;
import ru.rbt.barsgl.gwt.client.account.AccountFormTech;
import ru.rbt.barsgl.gwt.client.accountPl.PlAccountForm;
import ru.rbt.barsgl.gwt.client.account_ofr.OfrAccountForm;
import ru.rbt.barsgl.gwt.client.audit.AuditForm;
import ru.rbt.barsgl.gwt.client.backvalue.BackValueForm;
import ru.rbt.barsgl.gwt.client.bal.OndemandBalanceUnloadForm;
import ru.rbt.barsgl.gwt.client.checkCardsRem.CheckCardRemForm;
import ru.rbt.barsgl.gwt.client.dict.*;
import ru.rbt.barsgl.gwt.client.events.ae.*;
import ru.rbt.barsgl.gwt.client.loader.FullLoaderControlForm;
import ru.rbt.barsgl.gwt.client.operBackValue.OperAuthBVForm;
import ru.rbt.barsgl.gwt.client.operBackValue.OperNotAuthBVForm;
import ru.rbt.security.gwt.client.monitoring.Monitor;
import ru.rbt.barsgl.gwt.client.operation.OperationPostingForm;
import ru.rbt.barsgl.gwt.client.operationTemplate.OperationTemplateForm;
import ru.rbt.barsgl.gwt.client.operday.BufferSyncForm;
import ru.rbt.barsgl.gwt.client.operday.OperDayForm;
import ru.rbt.barsgl.gwt.client.pd.PDForm;
import ru.rbt.barsgl.gwt.client.pd.PostingForm;
import ru.rbt.barsgl.gwt.client.pd.PostingFormTech;
import ru.rbt.barsgl.gwt.client.security.AppUserForm;
import ru.rbt.security.gwt.client.security.LoginFormHandler;
import ru.rbt.barsgl.gwt.client.security.RoleForm;
import ru.rbt.tasks.gwt.client.tasks.TasksFormNew;
import ru.rbt.barsgl.gwt.core.LocalDataStorage;
import ru.rbt.barsgl.gwt.core.forms.IDisposable;
import ru.rbt.barsgl.gwt.core.resources.ImageConstants;
import ru.rbt.barsgl.shared.RpcRes_Base;
import ru.rbt.shared.access.UserMenuItemWrapper;
import ru.rbt.shared.access.UserMenuWrapper;
import ru.rbt.shared.enums.UserMenuCode;
import ru.rbt.shared.enums.UserMenuType;

import java.util.ArrayList;
import java.util.List;
import ru.rbt.security.gwt.client.formmanager.IMenuBuilder;
import ru.rbt.security.gwt.client.security.SecurityEntryPoint;

/**
 * Created by akichigi on 27.04.16.
 */
public class MenuBuilder implements IMenuBuilder {
    private final MenuItemTemplate menuTemplate = GWT.create(MenuItemTemplate.class);

    private UserMenuWrapper rootWrapper;
    private DockLayoutPanel dataPanel;

  //menuBuilder = new MenuBuilder(menuWrapper, dataPanel).build(menuBar);
  public MenuBuilder() {
  }

  @Override
  public void init(UserMenuWrapper wrapper, DockLayoutPanel dataPanel) {
        this.dataPanel = dataPanel;

        List<UserMenuItemWrapper> rootList;
        if (wrapper == null) {
            rootList = new ArrayList<>();
            rootWrapper = new UserMenuWrapper(rootList);
        } else {
            rootWrapper = wrapper;
            rootList = rootWrapper.getRootElements();
        }

        UserMenuItemWrapper tmp = null;
        // System
        for (UserMenuItemWrapper item : rootList) {
            if (item.getMenuCode() == UserMenuCode.SystemMenu) {
                tmp = item;
                break;
            }
        }
        List<UserMenuItemWrapper> systemList;
        if (tmp == null) {
            systemList = new ArrayList<>();
            systemList.add(new UserMenuItemWrapper(-1, "Выход", UserMenuCode.SystemExit, UserMenuType.L));
            tmp = new UserMenuItemWrapper(1, "Система", UserMenuCode.SystemMenu, UserMenuType.N);
            tmp.setChildren(systemList);
            rootList.add(0, tmp);

        } else {
            systemList = tmp.getChildren();
            systemList.add(new UserMenuItemWrapper(-1, "-", UserMenuCode.Separator, UserMenuType.L));
            systemList.add(new UserMenuItemWrapper(-1, "Выход", UserMenuCode.SystemExit, UserMenuType.L));
        }

        // Help
        ArrayList<UserMenuItemWrapper> aboutList = new ArrayList<>();
        aboutList.add(new UserMenuItemWrapper(-1, " О программе...", UserMenuCode.HelpAbout, UserMenuType.L));
        tmp = new UserMenuItemWrapper(-1, "Справка", UserMenuCode.Help, UserMenuType.N);
        tmp.setChildren(aboutList);
        rootList.add(tmp);

        // Separators
        for(int i = rootList.size()-1; i > 0; i--){
            rootList.add(i, new UserMenuItemWrapper(-1, "-", UserMenuCode.Separator, UserMenuType.L));
        }
    }

    private MenuItem createItem(UserMenuItemWrapper wrapper){
        MenuItem item;
        switch (wrapper.getMenuCode()) {
            case SystemExit: return new MenuItem("Выход", false, new Command() {
                @Override
                public void execute() {
                    LocalDataStorage.clear();
                    LoginFormHandler.logoff();
                }
            });
            case HelpAbout: return new MenuItem(menuTemplate.createItem(ImageConstants.INSTANCE.about16().getSafeUri(),
                    SafeHtmlUtils.fromString(" О программе...")), new Command(){
                @Override
                public void execute() {
                    AboutForm aboutForm = new AboutForm();
                    aboutForm.show(SecurityEntryPoint.getDatabaseVersion());
                }
            });
            case Task: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    //formLoad(new TasksForm());
                    BarsGLEntryPoint.propertiesService.getEnvProperty("java:app/env/SchedTableName", new AsyncCallback<RpcRes_Base<String>>() {
                        @Override
                        public void onSuccess(RpcRes_Base<String> result) {
                            final String schedTableName = result.getResult();
                            formLoad(new TasksFormNew() {
                                @Override
                                protected String prepareSql() {
                                    return "SELECT * FROM " + schedTableName;
                                }

                            });
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            throw new RuntimeException(caught);
                        }
                    });
                }
            });

            case LoaderControl: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new FullLoaderControlForm());
                }
            });

            case Operday: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new OperDayForm());
                }
            });
            case User: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new AppUserForm());
                }
            });
            case Role: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new RoleForm());
                }
            });
            case Backvalue: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new BackValueForm());
                }
            });
            case Audit: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new AuditForm());
                }
            });
            case PLAccountAcctype: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new PlAccountForm());
                }
            });
            case PLAccountMidas: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new OfrAccountForm());
                }
            });
            case CBAccount: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new AccountForm());
                }
            });
            case TechAccount: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new AccountFormTech());
                }
            });
            case TechOperInpConfirm: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new OperTechInpConfirmForm());
                }
            });
            case Operation: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new OperationPostingForm());
                }
            });
            case Posting: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new PostingForm());
                }
            });
            case TechPosting: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new PostingFormTech());
                }
            });
            case OperSemiposting: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new PDForm());
                }
            });
            case ErrorView: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new LoadErrorForm());
                }
            });
            case ErrorHandling: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new LoadErrorHandlingForm());
                }
            });
            case UnloadAccountBalance: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new OndemandBalanceUnloadForm());
                }
            });
            case AEIncomeMsg: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new InEventsAE());
                }
            });

            case OperInpConfirm: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new OperInpConfirmForm());
                }
            });

            case FileIncomeMsg: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new BatchPostingForm());
                }
            });
            case FileIncomePkg: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new BatchPackageForm());
                }
            });
            case PostingSource: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new SourcesDeals());
                }
            });
            case TermCode: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new TypesOfTerms(false));
                }
            });
            case AccountingType: return  new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new AccountingType());
                }
            });
            case PlanAccountingType: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new AccountTypesByCategory());
                }
            });
            case PlanAccountOfr: return  new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new OfrSymbols());
                }
            });
            case PropertyType: return  new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new PropertyType());
                }
            });
            case Branch: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new Departments());
                }
            });
            case UnloadStamtConfig: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new StamtUnloadParamDict());
                }
            });

            case TemplateOper: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new OperationTemplateForm());
                }
            });

            case OperInpHistory: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new OperEventHistoryForm());
                }
            });

            case ProfitCentr: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad( new ProfitCenter());
                }
            });

            case BufferSync: return new MenuItem(BufferSyncForm.FORM_NAME, false, new Command() {
                @Override
                public void execute() {
                    formLoad( new BufferSyncForm());
                }
            });

            case AccTypeParts: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad( new AccTypeSection());
                }
            });

            case AcodMidas: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad( new Acod());
                }
            });

            case Monitoring: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad( new Monitor());
                }
            });

            case CheckCardsRemains: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad( new CheckCardRemForm());
                }
            });

            case TechOperInpHistory: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad( new OperTechEventHistoryForm());
                }
            });

            case OperBackValue: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad( new OperTechEventHistoryForm());
                }
            });

            case OperNotAuthBV: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new OperNotAuthBVForm());
                }
            });

            case OperAuthBV: return new MenuItem(wrapper.getMenuName(), false, new Command() {
                @Override
                public void execute() {
                    formLoad(new OperAuthBVForm());
                }
            });

            default: return getUnSupportedMenuItem();
        }
    }

    private MenuItem getUnSupportedMenuItem(){
        return new MenuItem("* Ошибка!!! *", false, new Command() {
            @Override
            public void execute() {
                Window.alert("Неизвестный (нереализованный) пункт меню.");
            }
        });
    }

    public MenuBuilder build(MenuBar menu){
        menu.setAnimationEnabled(true);

        for (UserMenuItemWrapper itemWrapper: rootWrapper.getRootElements()){
            createMenuElement(menu, itemWrapper);
        }
        return this;
    }

    private void createMenuElement(MenuBar menu, UserMenuItemWrapper itemWrapper) {
        if (itemWrapper.getType() == UserMenuType.L) {
            if (itemWrapper.getMenuCode() == UserMenuCode.Separator){
                menu.addSeparator();
            } else{
                menu.addItem(createItem(itemWrapper));
            }
        } else {
            MenuBar newMenu = new MenuBar(true);
            for (UserMenuItemWrapper item : itemWrapper.getChildren()) {
                createMenuElement(newMenu, item);
            }
            menu.addItem(itemWrapper.getMenuName(), newMenu);
        }
    }

    public void formLoad(Widget form){
        if (dataPanel.getWidgetCount() == 1){
            Widget w = dataPanel.getWidget(0);
            if (w instanceof IDisposable) {
                ((IDisposable) w).dispose();
            }
        }
        dataPanel.clear();
        dataPanel.add(form);
    }
}

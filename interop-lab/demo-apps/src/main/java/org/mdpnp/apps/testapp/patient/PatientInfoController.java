package org.mdpnp.apps.testapp.patient;

import ice.MDSConnectivity;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import org.mdpnp.apps.testapp.Device;
import org.mdpnp.apps.testapp.DeviceListModel;
import org.mdpnp.devices.MDSHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

public class PatientInfoController implements ListChangeListener<Device>, MDSHandler.Connectivity.MDSListener {

    private static final Logger log = LoggerFactory.getLogger(PatientInfoController.class);

    private DeviceListModel deviceListDataModel;
    private EMRFacade       emr;
    private MDSHandler      mdsConnectivity;

    @FXML Button connectBtn;
    @FXML TableView<PatientInfo> patientView;
    @FXML TableView<Device> deviceView;

    @FXML Button createNewPatient;
    @FXML TextField newPatientMRN;
    @FXML TextField newPatientFirstName;
    @FXML TextField newPatientLastName;
    @FXML ComboBox<PatientInfo.Gender> newPatientGender;
    @FXML DatePicker newPatientDOB;

    @FXML TableView<DevicePatientAssociation> associationTableView;
    @FXML TableColumn<DevicePatientAssociation, String> associationTableActionColumn;

    protected ObservableList<DevicePatientAssociation> associationModel = FXCollections.observableArrayList();
    protected ObservableList<Device> deviceListModel = FXCollections.observableArrayList();
    protected ObservableList<PatientInfo> patientListModel = FXCollections.observableArrayList();
    protected ObservableList<PatientInfo.Gender> genderListModel = FXCollections.observableArrayList();
    {
        genderListModel.add(PatientInfo.Gender.male);
        genderListModel.add(PatientInfo.Gender.female);
    }
    public DeviceListModel getDeviceListDataModel() {
        return deviceListDataModel;
    }

    public void setDeviceListDataModel(DeviceListModel dlm) {
        deviceListDataModel = dlm;
    }

    public EMRFacade getEmr() {
        return emr;
    }

    public void setEmr(EMRFacade db) {
        emr = db;
    }

    public MDSHandler getMdsConnectivity() {
        return mdsConnectivity;
    }

    public void setMdsConnectivity(MDSHandler mdsConnectivity) {
        this.mdsConnectivity = mdsConnectivity;
    }

    /**
     * UI callback from the table when row is selected.
     * @param oldValue
     * @param newValue
     */
    void onAssociationSelected(DevicePatientAssociation oldValue, DevicePatientAssociation newValue)
    {
        /*
        if(oldValue != null) {
            uniqueDeviceIdentifier.textProperty().unbindBidirectional(oldValue.deviceIdentifierProperty());
            patientIdentifier.textProperty().unbindBidirectional(oldValue.lastNameProperty());
            patientName.textProperty().unbindBidirectional(oldValue.patientIdentifierProperty());
        }
        
        uniqueDeviceIdentifier.textProperty().bindBidirectional(newValue.deviceIdentifierProperty());
        patientIdentifier.textProperty().bindBidirectional(newValue.lastNameProperty());
        patientName.textProperty().bindBidirectional(newValue.patientIdentifierProperty());
        */
    }


    public void removeDeviceAssociation(DevicePatientAssociation assoc)  {
        emr.deleteDevicePatientAssociation(assoc);
        associationModel.remove(assoc);
        deviceListModel.add(assoc.getDevice());
    }

    void addDeviceAssociation(Device d, PatientInfo p) {
        DevicePatientAssociation assoc = associate(d, p);
        associationModel.add(assoc);
        deviceListModel.remove(d);
    }

    private DevicePatientAssociation associate(Device d, PatientInfo p)  {

        DevicePatientAssociation dpa = emr.updateDevicePatientAssociation(new DevicePatientAssociation(d, p));

        ice.MDSConnectivityObjective mds=new ice.MDSConnectivityObjective();
        mds.unique_device_identifier = d.getUDI();
        mds.partition = p.getLastName();
        mdsConnectivity.publish(mds);

        return dpa;
    }

    public void handleDeviceLifecycleEvent(Device d, boolean added)  {

        if(added && !inUse(d))
            deviceListModel.add(d);
        else
            deviceListModel.remove(d);
    }

    /**
     * @return true if device already associated with the patient.
     */
    private boolean inUse(final Device d) {
        FilteredList<DevicePatientAssociation> l = associationModel.filtered(new Predicate<DevicePatientAssociation>() {
            @Override
            public boolean test(DevicePatientAssociation devicePatientAssociation) {
                return devicePatientAssociation.isForDevice(d);
            }
        });
        return l.size()!=0;
    }

    /**
     * A table cell containing a button deleting the row.
     **/
    private class DeleteAssociationCell extends TableCell<DevicePatientAssociation, String> {

        final Button button = new Button();
        // pads and centers the add button in the cell.
        final StackPane paddedButton = new StackPane();

        DeleteAssociationCell() {

            try {
                ImageView img = new ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream("disconnect.png")));
                img.setFitHeight(15.0);
                img.setFitWidth(15.0);
                button.setGraphic(img);
            } catch (Exception noImg) {
                log.error("Failed to load image for disconnect button", noImg);
                button.setText("Disconnect");
            }
            paddedButton.setPadding(new Insets(1));
            paddedButton.getChildren().add(button);

            button.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    TableRow<DevicePatientAssociation> row = getTableRow();
                    DevicePatientAssociation obj = row.getTableView().getItems().get(row.getIndex());
                    removeDeviceAssociation(obj);
                }
            });
        }

        /**
         * places a button in the row only if the row is not empty.
         **/
        @Override
        protected void updateItem(String item, boolean empty) {
            int row = getTableRow().getIndex();
            super.updateItem(item, empty);
            if (!empty) {
                log.info("render " + row + "->" + item);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(paddedButton);
            }
            else {
                setContentDisplay(ContentDisplay.TEXT_ONLY);
                setGraphic(null);
            }
        }
    }


    /**
     * called by the IOC framework when the component is all wired up and ready to go
     * @throws Exception when things go wrong
     */
    public void start() throws Exception
    {
        deviceListDataModel.getContents().addListener(this);

        Callback<TableColumn<DevicePatientAssociation, String>, TableCell<DevicePatientAssociation, String>> columnFac =
            new Callback<TableColumn<DevicePatientAssociation, String>, TableCell<DevicePatientAssociation, String>>() {
            @Override public TableCell<DevicePatientAssociation, String> call(TableColumn<DevicePatientAssociation, String> personBooleanTableColumn) {
                return new DeleteAssociationCell();
            }
        };

        associationTableActionColumn.setCellFactory(columnFac);

        associationTableView.setItems(associationModel);
        deviceView.setItems(deviceListModel);
        newPatientGender.setItems(genderListModel);
        newPatientGender.setValue(PatientInfo.Gender.male);
        patientView.setItems(patientListModel);

        associationTableView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<DevicePatientAssociation>() {
            @Override
            public void changed(ObservableValue<? extends DevicePatientAssociation> observable,
                                DevicePatientAssociation oldValue,
                                DevicePatientAssociation newValue) {
                onAssociationSelected(oldValue, newValue);
            }
        });

        connectBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Device d = getSelectedDevice();
                PatientInfo p = getSelectedPatient();
                if (d != null && p != null) {
                    proposeDeviceAssociation(d, p);
                }
            }
        });

        createNewPatient.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                String mrn   = Long.toHexString(System.currentTimeMillis());
                String lName = newPatientLastName.getText();
                String fName = newPatientFirstName.getText();
                LocalDate localDate = newPatientDOB.getValue();

                if(fName.trim().length() != 0 &&
                   lName.trim().length() != 0 &&
                   mrn.trim().length() != 0 &&
                   localDate != null) {

                    Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

                    PatientInfo pi = new PatientInfo(mrn, fName, lName);
                    pi.setDob(date);
                    pi.setGender(newPatientGender.getValue());

                    if(addPatient(pi)) {
                        newPatientLastName.setText("");
                        newPatientFirstName.setText("");
                    }
                }
            }
        });
        List<PatientInfo> p =  emr.getPatients();
        patientListModel.addAll(p);

        mdsConnectivity.addConnectivityListener(this);
    }

    PatientInfo getSelectedPatient() {
        return patientView.getSelectionModel().getSelectedItem();
    }

    Device getSelectedDevice() {
        return deviceView.getSelectionModel().getSelectedItem();
    }

    void setConnectHandler(EventHandler<ActionEvent> a) {
        connectBtn.setOnAction(a);
    }

    public void proposeDeviceAssociation(Device d, PatientInfo p) {
        ice.MDSConnectivityObjective mds=new ice.MDSConnectivityObjective();
        mds.unique_device_identifier = d.getUDI();
        mds.partition = p.getLastName();
        mdsConnectivity.publish(mds);
    }

    private boolean addPatient(PatientInfo pi) {
        if(emr.createPatient(pi)) {
            patientView.getItems().add(pi);
            return true;
        }
        return false;
    }

    /**
     * called by the IOC framework when the component is about to be shut down.
     * @throws Exception when things go wrong
     */
    public void stop() throws Exception {
        mdsConnectivity.removeConnectivityListener(this);
        deviceListDataModel.getContents().removeListener(this);
    }

    @Override
    public void handleDataSampleEvent(MDSHandler.Connectivity.MDSEvent evt) {

        MDSConnectivity state = (MDSConnectivity)evt.getSource();
        Device d = findDevice(state.unique_device_identifier, deviceView.getItems());
        PatientInfo p = findPatient(state.partition, patientView.getItems());
        if(d != null && p != null)
            addDeviceAssociation(d, p);
    }

    private static Device findDevice(String uid, ObservableList<Device> items) {
        for (Device item : items) {
            if(item.getUDI().equals(uid))
                return item;
        }
        return null;
    }

    private static PatientInfo findPatient(String partition, ObservableList<PatientInfo> items) {
        for (PatientInfo item : items) {
            if(item.getLastName().equals(partition))
                return item;
        }
        return null;
    }

    @Override
    public void onChanged(Change<? extends Device> c) {
        while (c.next()) {
            if (c.wasAdded() || c.wasRemoved()) {
                for (Device d : c.getRemoved()) {
                    log.info("Device Removed", d.toString());
                    handleDeviceLifecycleEvent(d, false);
                }
                for (Device d : c.getAddedSubList()) {
                    log.info("Device Added", d.toString());
                    handleDeviceLifecycleEvent(d, true);
                }
            }
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    public PatientInfoController() {
        super();
    }

}
package steps;

import api.APILibrary;
import api.EndPoints;
import api.TokenAPI;
import cucumber.api.java.After;
import org.json.JSONArray;
import cucumber.api.java.en.And;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import entities.ConferenceRoom;
import entities.Location;
import mongodb.DataBaseDriver;
import org.json.JSONObject;
import ui.pages.admin.ConferenceRoomsPage;
import ui.pages.admin.LocationPage;

import entities.Resource;
import framework.UIMethods;
import junit.framework.Assert;
import ui.pages.admin.ResourceAssociatePage;

import ui.pages.admin.RoomSettingsPage;
import ui.pages.admin.HomePage;
import utils.CredentialManager;
import utils.LeftBarOptions;

/**
 * Author: Jorge Avila
 * Date: 12/3/15
 */
public class ConferenceRoomSteps {

    private HomePage homePage;
    private ConferenceRoomsPage conferenceRoomsPage;
    private RoomSettingsPage roomInfoPage;
    private LocationPage locationPage;
    private ConferenceRoom conferenceRoom;
    private Location location;
    private Resource resource;
    private ResourceAssociatePage resoureAssociatePage;
    private JSONObject response;
    private String token;

    public ConferenceRoomSteps(ConferenceRoom conferenceRoom){
        homePage = new HomePage();
        resource = new Resource();
        this.conferenceRoom = conferenceRoom;
        this.location = new Location();
        token = obtainToken();
    }

    @Given("^I open the Room \"(.*?)\" from the Conference Room$")
    public void openRoomFromConferenceRoom(String roomName) {
        conferenceRoom.setDisplayName(roomName);
        conferenceRoom.setCustomDisplayName(roomName);
        conferenceRoomsPage = homePage.getLeftMenuPanel()
                                      .clickOnConferenceRooms("Conference Rooms");

        roomInfoPage = conferenceRoomsPage.openConferenceRoomSettings(conferenceRoom.getCustomDisplayName());
    }

    @When("^I assign the current Room to the Location \"(.*?)\"$")
    public void assignRoomToALocation(String locationName){
        assignLocationToRoom(locationName);
    }

    private void assignLocationToRoom(String locationName) {
        location.setName(locationName);
        location.setDisplayName(locationName);
        roomInfoPage.assignLocation(locationName);
    }


    @Then("^the current room is associated to the Location defined in the Locations page$")
    public void isAssociatedRoomToLocationRoomPage(){
        locationPage = homePage.getLeftMenuPanel()
                .clickOnLocationPage("Locations");
        boolean existAssociated = locationPage.verifyIfExistLocationAssociation(location, conferenceRoom);
        Assert.assertTrue(existAssociated);
    }

    @When("^I edit the following info: Display Name \"(.*?)\", code \"(.*?)\" and capacity \"(.*?)\"$")
    public void editInfoConferenceRoom(String displayName, String roomCode, String roomCapacity){
        conferenceRoom.setCustomDisplayName(displayName);
        conferenceRoom.setCode(roomCode);
        conferenceRoom.setCapacity(roomCapacity);
        roomInfoPage.fillForm(conferenceRoom);
    }

    @Then("^the info edited should be obtained by API request for the Room \"(.*?)\"$")
    public void isTheInfoEditedObtainedByAPI(String roomName){

    }

    @Given("^I have created a resource with name \"(.*?)\", customName \"(.*?)\"$")
    public void createResource(String resourceName, String resourceDisplayName){
        resource.setName(resourceName);
        resource.setCustomName(resourceDisplayName);
        resource.setFontIcon("fa fa-desktop");

        JSONObject jsonResource = new JSONObject();
        jsonResource.put("name", resourceName);
        jsonResource.put("customName", resourceDisplayName);
        jsonResource.put("fontIcon", "fa fa-desktop");
        jsonResource.put("description", "");
        jsonResource.put("from", "");

        String endPoint = EndPoints.RESOURCE;
        JSONObject response = APILibrary.getInstance().post(jsonResource, token, endPoint);

        conferenceRoom.addResource(resource);
    }

    @And("^I go to \"(.*?)\" page$")
    public void goToConferenceRoomPage(String namePage){
        conferenceRoomsPage = homePage.getLeftMenuPanel().clickOnConferenceRooms(namePage);
    }
    @And("^I select the resource button in the header page$")
    public void displayResourceInTableConferenceRoom(){
        conferenceRoomsPage.clickOnResourcesDisplayButton(resource);
    }
    @And("^I select the Resource Association Tab$")
    public void selectResourceAssociateTab(){
        resoureAssociatePage = roomInfoPage.clickOnResourceAssociateTab();
    }
    @And("^I add \"(.*?)\" Resource to the Room$")
    public void addResourcesToRoom(String quantity){
        resource.setQuantity(quantity);
        resoureAssociatePage.clickOnAddResources(resource)
                            .typeQuantityResources(resource);
        conferenceRoomsPage = resoureAssociatePage.clickOnSaveButton();
    }
    @Then("^the resource and quantity should be displayed for the room in the list$")
    public void verifyResourceAndQuantity(){
        conferenceRoomsPage.makeSureResourcesIsSelect(resource);
        boolean verificationIcon = conferenceRoomsPage.isTheResourceCorrect(resource, conferenceRoom);
        boolean verificationQuantity = conferenceRoomsPage.isTheSameQuantityOfResources(resource, conferenceRoom);
        Assert.assertTrue("The resource icon is the same that it's assigned ", verificationIcon);
        Assert.assertTrue("The quantity is the same that was assigned", verificationQuantity);
    }
    @When("^I pressing the disable button$")
    public void disableRoom(){
        conferenceRoom.setEnabled(false);
        conferenceRoomsPage = roomInfoPage.clickOnPowerOffRoomButton(conferenceRoom);
        UIMethods.switchPages(LeftBarOptions.CONFERENCE_ROOMS.getToPage());
    }
    @Then("^The current Room should be disable$")
    public void verifyRoomDisable(){
        Assert.assertTrue("The room was disable correctly", conferenceRoomsPage.isRoomEnable(conferenceRoom));
    }
    @And("^the information updated in the room should be obtained by API$")
    public void verifyRoomIsDisableByAPI(){
        DataBaseDriver.getInstance().createConnectionToDB(CredentialManager.getInstance().getIp());
        String id = DataBaseDriver.getInstance().getKeyValue("rooms", "displayName", conferenceRoom.getDisplayName(), "_id");
        conferenceRoom.setId(id);
        DataBaseDriver.getInstance().closeConnectionToDB();

        String endPoint = EndPoints.ROOM_BY_ID.replace("#id#", id);
        JSONObject response = APILibrary.getInstance().getById(endPoint);

        Assert.assertFalse("the room is disabled", response.getBoolean("enabled"));
    }
    @And("^the Room obtain by api should be contain the resource id$")
    public void verifyResourceInRoom(){
        DataBaseDriver.getInstance().createConnectionToDB(CredentialManager.getInstance().getIp());
        String id = DataBaseDriver.getInstance().getKeyValue("rooms", "displayName", conferenceRoom.getDisplayName(), "_id");
        conferenceRoom.setId(id);
        DataBaseDriver.getInstance().closeConnectionToDB();

        String endPoint = EndPoints.ROOM_BY_ID.replace("#id#", conferenceRoom.getId());
        JSONObject response = APILibrary.getInstance().getById(endPoint);
        JSONArray resources = (JSONArray) response.get("resources");
        String resourceID = null;
        for (int ind = 0; ind<resources.length(); ind++){
             resourceID = resources.getJSONObject(ind).getString("resourceId");
            System.out.println(resourceID);
            System.out.println(resource.getId());
        }
        Assert.assertEquals("the resource id is the same that assigned", resourceID, resource.getId());
    }
    @And("^the room obtain by api should be contain the quantity assign$")
    public void verifyQuantityResourcesInRoom(){
        String endPoint = EndPoints.ROOM_BY_ID.replace("#id#", conferenceRoom.getId());
        JSONObject response = APILibrary.getInstance().getById(endPoint);

        JSONArray resources = (JSONArray) response.get("resources");
        String resourceQuantity = null;
        for (int ind = 0; ind<resources.length(); ind++){
            resourceQuantity = resources.getJSONObject(ind).getString("quantity");
        }

        Assert.assertEquals("the quantity the resouces assigned in the room is the same that was assigned", resourceQuantity, resource.getQuantity());
    }

    @Given("^I have a Room with name \"([^\"]*)\" that is associated with the Location \"([^\"]*)\"$")
    public void createResourceWithLocation(String roomName, String locationName) throws Throwable {
        JSONObject location = new JSONObject();
        location.put("name", locationName);
        location.put("customName", locationName);

        String endPoint = EndPoints.LOCATIONS;
        response = APILibrary.getInstance().post(location, token, endPoint);

        DataBaseDriver.getInstance().createConnectionToDB(CredentialManager.getInstance().getIp());
        conferenceRoom.setId(DataBaseDriver.getInstance().getKeyValue("rooms", "displayName", roomName, "_id"));
        DataBaseDriver.getInstance().closeConnectionToDB();

        JSONObject updateRoom = new JSONObject();
        updateRoom.put("locationId", response.getString("_id"));
        String roomsEndPoint = EndPoints.ROOM_BY_ID.replace("#id#", conferenceRoom.getId());
        response = APILibrary.getInstance().put(updateRoom, token, roomsEndPoint);
    }

    @And("^I have a Location with name \"([^\"]*)\"$")
    public void createNewLocation(String name) throws Throwable {
        location.setName(name);
        location.setDisplayName(name);
        JSONObject location = new JSONObject();
        location.put("name", name);
        location.put("customName", name);
        String locationsEndPoint = EndPoints.LOCATIONS;
        response = APILibrary.getInstance().post(location, token, locationsEndPoint);
    }

    @Then("^the current Room should be moved to the new Location$")
    public void isRoomAssociatedToLocation() throws Throwable {
        isAssociatedRoomToLocationRoomPage();
    }

    @And("^the current Room should be obtained by API request associated with the location$")
    public void isTheRoomAObtainedByAPI() throws Throwable {
        String endPoint = EndPoints.RESOURCE_BY_ID.replace("#id#", conferenceRoom.getId());
        JSONObject response = APILibrary.getInstance().getById(endPoint);
        Assert.assertEquals(conferenceRoom.getLocationId(), response.getString("locationId"));
    }

    @And("^I assign the current Room to the new Location \"([^\"]*)\"$")
    public void I_assign_the_current_Room_to_the_new_Location(String locationName) throws Throwable {
        assignRoomToALocation(locationName);
    }

//    @After(value = "locationRoom")
    public void deleteTokenLocation(){
        String endPoint = EndPoints.LOCATION_BY_ID.replace("#id#", location.getId());
        APILibrary.getInstance().delete(endPoint);
    }

    /**
     * Get the token to use in the API methods
     * @return a string value that is the Toke
     */
    private String obtainToken(){
        String username = CredentialManager.getInstance().getUserNameAdmin();
        String password = CredentialManager.getInstance().getPasswordAdmin();
        String token = TokenAPI.getInstance().getToken(username, password, "local");
        return token;
    }
}

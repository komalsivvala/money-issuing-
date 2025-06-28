package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Starting with TDD (Test Driven Development) approach
 * Note: Remember design a failing test first and then create a success one
 */

/*
 * start our Spring Boot application and make it available for our test to
 * perform requests to it
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashcardApplicationTests {

  @Autowired
  private TestRestTemplate restTemplate;/*
   * Injecting a test helper that’ll allow us to make HTTP requests to the
   * locally running application
   */

  private CashCard[] cashCards;

  @BeforeEach
  void setUp() {/* Creating pre-defined CashCard objects */
    cashCards =
      new CashCard[] {
        new CashCard(1L, 123.45, "LeudiX1"),
        new CashCard(2L, 100.50, "Sarah"),
        new CashCard(3L, 325.33, "Lucy2"), //
      };
    /*
     * Adding multiple CashCard objects via the POST method(Endpoint) in the
     * API
     */
    restTemplate
      .withBasicAuth(
        "LeudiX1",
        "leo123"
      )/* Added basic authentication for LeudiX1 user */
      .postForEntity("/cashcards", cashCards[0], Void.class);

    restTemplate
      .withBasicAuth(
        "Sarah",
        "sara123"
      )/* Added basic authentication for Sarah user */
      .postForEntity("/cashcards", cashCards[1], Void.class);

    restTemplate
      .withBasicAuth(
        "Lucy2",
        "lucy123"
      )/* Added basic authentication for Lucy2 user */
      .postForEntity("/cashcards", cashCards[2], Void.class);
  }

  @Test
  void shouldReturnACashCardWhenDataIsSaved() {
    /*
     * Using restTemplate to make an HTTP GET request to our application endpoint
     * /cashcards/1
     */
    ResponseEntity<String> response = restTemplate
      .withBasicAuth(
        "LeudiX1",
        "leo123"
      )/* added basic authentication for LeudiX1 user */
      .getForEntity("/cashcards/1", String.class);

    /* Expecting the HTTP reponse status code to be 200 OK */
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    /*
     * Converts the response String into a JSON-aware object with lots of helper
     * methods
     */
    DocumentContext dContext = JsonPath.parse(response.getBody());

    Number id = dContext.read("$.id");
    Double amount = dContext.read("$.amount");
    assertThat(id).isEqualTo(1);
    assertThat(amount).isEqualTo(123.45);
  }

  @Test
  void shouldNotReturnACashCardWithAnUnknownId() {
    ResponseEntity<String> response = restTemplate
      .withBasicAuth("LeudiX1", "leo123")
      .getForEntity("/cashcards/1000", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isBlank();
  }

  @Test
  void shouldCreateANewCashCard() {
    /*
     * Database will create and manage all unique CashCard.id values for us. We
     * shouldn't provide one. Also we shouldn't provide a CashCard owner cause
     * we risk allowing users to create CashCards for someone else.
     *
     * Only the authenticated, authorized Principal owns the CashCards they are creating
     */
    CashCard cashCard = new CashCard(null, 100.0, null);

    ResponseEntity<Void> response = restTemplate
      .withBasicAuth("LeudiX1", "leo123")
      .postForEntity("/cashcards", cashCard, Void.class);

    /*
     * Expecting the HTTP response status code to be 201 CREATED, which is
     * semantically
     * correct if our API creates a new CashCard from our request.
     */
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    /*
     * Obtaninig the location of the recently created new CashCard resource through
     * the Response Header
     */
    URI locationOfNewCashCard = response.getHeaders().getLocation();
    /* Performing a GET to access the newly created CashCard resource */
    ResponseEntity<String> responseToNewCashCard = restTemplate
      .withBasicAuth("LeudiX1", "leo123")
      .getForEntity(locationOfNewCashCard, String.class);

    assertThat(responseToNewCashCard.getStatusCode()).isEqualTo(HttpStatus.OK);

    DocumentContext docContxt = JsonPath.parse(responseToNewCashCard.getBody());

    Number id = docContxt.read("$.id");
    Double amount = docContxt.read("$.amount");
    String owner = docContxt.read("$.owner");

    assertThat(id).isNotNull();
    assertThat(amount).isEqualTo(100.0);
    assertThat(owner).isEqualTo("LeudiX1");
  }

  /*
   * We should be able to list all CashCards owned by an authenticated user(IF POSSES ONE)
   */
  @Test
  void shouldReturnAllCashCardsOwnedByAuthUserWhenListIsRequested() {
    ResponseEntity<String> response2 = restTemplate
      .withBasicAuth("LeudiX1", "leo123")
      .getForEntity("/cashcards", String.class);

    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);

    DocumentContext docContxt = JsonPath.parse(response2.getBody());

    int cashCardCount = docContxt.read(
      "$.length()"
    );/* calculates the length of the array */

    assertThat(cashCardCount).isEqualTo(1);

    JSONArray ids = docContxt.read(
      "$..id"
    );/* retrieves the list of all id values returned */
    JSONArray amounts = docContxt.read(
      "$..amount"
    );/* retrieves the list of all amount values returned */

    /* while the list contain everything I assert, the order does not matter */
    assertThat(ids).containsExactlyInAnyOrder(1);
    assertThat(amounts).containsExactlyInAnyOrder(123.45);
  }

  /*
   * Testing paging (Showing 1 page at a time for each CashCard element)
   */
  @Test
  void shouldReturnPagedCashCardsWhenPagingIsRequested() {
    ResponseEntity<String> responseEntity = restTemplate
      .withBasicAuth("LeudiX1", "leo123")
      .getForEntity("/cashcards?page=0&size=1", String.class);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

    DocumentContext docContext = JsonPath.parse(responseEntity.getBody());
    JSONArray page = docContext.read("$[*]");

    assertThat(page.size()).isEqualTo(1);
  }

  /*
   * Testing sorting (Order by amount DESC)
   */
  @Test
  void shouldReturnASortedPageofCashCards() {
    ResponseEntity<String> responseEntity = restTemplate
      .withBasicAuth("LeudiX1", "leo123")
      .getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext docContext = JsonPath.parse(responseEntity.getBody());
    JSONArray page = docContext.read("$[*]");

    assertThat(page.size()).isEqualTo(1);

    double amount = docContext.read("$[0].amount");
    assertThat(amount).isEqualTo(123.45);
  }

  /*
   * Testing sorting with default values (Order by amount ASC)
   */
  @Test
  void shouldReturnASortedPageofCashCardsWithNoParametersAndUseDefaultValues() {
    ResponseEntity<String> responseEntity = restTemplate
      .withBasicAuth("LeudiX1", "leo123")
      .getForEntity("/cashcards", String.class);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

    DocumentContext docContext = JsonPath.parse(responseEntity.getBody());
    JSONArray page = docContext.read("$[*]");

    assertThat(page.size()).isEqualTo(1);

    JSONArray amounts = docContext.read("$[*].amount");
    assertThat(amounts).containsExactly(123.45);
  }

  /*
   * A user who don't posses a cash card shouldn't be able to get access to Family Card
   */
  @Test
  void shouldRejectAnyUserWhoAreNotACardOwner() {
    ResponseEntity<String> responseEntity = restTemplate
      .withBasicAuth("Lucy2", "lucy123")
      .getForEntity("/cashcards/3", String.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  /*
   * asserts that my API returns a 404 NOT FOUND when a user attempts to access
   * a Cash Card they do not own, so the CashCard existence it's not revealed to
   * the user
   */
  @Test
  void usersShouldNotHaveAccessToOtherUsersCards() {
    ResponseEntity<String> responseEntity = restTemplate
      .withBasicAuth("Sarah", "sara123")
      .getForEntity("/cashcards/3", String.class); //Lucy's CashCard data
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  /*
   * Update An Existing CashCard once its owner has been authenticated and authorized by the system
   */
  @Test
  @DirtiesContext //Cleaning the contexts executed before
  void shouldUpdateAnExistingCashCard() {
    CashCard cashCard = new CashCard(null, 500.50, null);

    ResponseEntity<Void> responseEntity = restTemplate
      .withBasicAuth("Sarah", "sara123")
      .exchange(
        "/cashcards/2",
        HttpMethod.PUT,
        new HttpEntity<>(cashCard),
        Void.class
      ); //restTemplate.putForEntity() doesn't exist!
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<String> getResponse = restTemplate
      .withBasicAuth("Sarah", "sara123")
      .getForEntity("/cashcards/2", String.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    DocumentContext dContext = JsonPath.parse(getResponse.getBody());
    Number id = dContext.read("$.id");
    Double amount = dContext.read("$.amount");

    assertThat(id).isEqualTo(2);
    assertThat(amount).isEqualTo(500.50);
  }

  /*
   * The API should not update a CashCard that doesn't exist.
   */
  @Test
  void shouldNotUpdateACashCardThatDoesNotExist() {
    CashCard unknownCashCard = new CashCard(null, 300.50, null);

    HttpEntity<CashCard> request = new HttpEntity<>(unknownCashCard);

    ResponseEntity<Void> responseEntity = restTemplate
      .withBasicAuth("Sarah", "sara123")
      .exchange("/cashcards/999", HttpMethod.PUT, request, Void.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  /*
   * The API should not update a CashCard that belongs to someone else.
   */
  @Test
  void shouldNotUpdateACashCardThatBelongsToSomeoneElse() {
    CashCard lucyCashCard = new CashCard(null, 255.50, null);

    HttpEntity<CashCard> request = new HttpEntity<>(lucyCashCard);
    ResponseEntity<Void> responseEntity = restTemplate
      .withBasicAuth("LeudiX1", "leo123")
      .exchange("/cashcards/3", HttpMethod.PUT, request, Void.class);
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  /*
   * The API should allows the deletion of a CashCard owned by a user
   */
  @Test
  @DirtiesContext //Add this annotation to all tests which change the data. If don't, then these tests could affect the result of other tests in the file
  void shouldDeleteAnExistingCashCardRecord() {
    ResponseEntity<Void> responseEntity = restTemplate
      .withBasicAuth("Sarah", "sara123")
      .exchange("/cashcards/2", HttpMethod.DELETE, null, Void.class); //Removing the CashCard

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<String> responseEntity2 = restTemplate
      .withBasicAuth("Sarah", "sara123")
      .getForEntity("/cashcards/2", String.class); //Try to GET the deleted CashCard

    assertThat(responseEntity2.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND); //Asserting that the CashCard was already deleted
  }

  @Test
  void shouldNotDeleteACashCardThatDoesNotExist() {
    ResponseEntity<Void> responseEntity = restTemplate
      .withBasicAuth("LeudiX1", "leo123")
      .exchange("/cashcards/10", HttpMethod.DELETE, null, Void.class);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  /*
   * The API should not allow the deletion of a CashCard to another user who is not his owner
   */
  @Test
  void shouldNotDeleteACashCardTheyDoNotOwn() {
    ResponseEntity<Void> deleteResponse = restTemplate
      .withBasicAuth("Sarah", "sara123")
      .exchange("/cashcards/1", HttpMethod.DELETE, null, Void.class);

    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    /*
     * Verifying that the record I tried unsuccessfully to delete is still ther
     */
    ResponseEntity<String> getresponse = restTemplate
      .withBasicAuth("LeudiX1", "leo123")
      .getForEntity("/cashcards/1", String.class);

    assertThat(getresponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}

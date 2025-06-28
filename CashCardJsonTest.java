package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

/**
 * Starting with TDD (Test Driven Development) approach
 * Note: Remember design a failing test first and then create a success one
 */
@JsonTest /*
           * Using the Jackson framework to provide extensive JSON testing and parsing
           * support
           */
public class CashCardJsonTest {

    @Autowired /* Create an object of the requested type specified below */
    private JacksonTester<CashCard> json; /* Wrapper to handle serialization and deserialization of JSON objects */

    @Autowired
    private JacksonTester<CashCard[]> jsonlist;

    private CashCard[] cashCards;

    @BeforeEach
    void setUp() {
        cashCards = new CashCard[] {
                new CashCard(99L, 123.45, "LeudiX1"),
                new CashCard(100L, 100.50, "LeudiX1"),
                new CashCard(101L, 325.33, "LeudiX1")
        };
    }

    @Test
    void cashCardSerializationTest() throws IOException {
        CashCard cashcard = cashCards[0];

        assertThat(json.write(cashcard)).hasJsonPathNumberValue("@.id");
        assertThat(json.write(cashcard)).extractingJsonPathNumberValue("@.id").isEqualTo(99);
        assertThat(json.write(cashcard)).hasJsonPathNumberValue("@.amount");
        assertThat(json.write(cashcard)).extractingJsonPathNumberValue("@.amount").isEqualTo(123.45);
        assertThat(json.write(cashcard)).hasJsonPathStringValue("@.owner");
        assertThat(json.write(cashcard)).extractingJsonPathStringValue("@.owner").isEqualTo("LeudiX1");
    }

    @Test
    void cashCardDeserializationTest() throws IOException {
        String expected = """
                {
                    "id": 99,
                    "amount": 123.45,
                    "owner":"LeudiX1"
                }
                """;
        assertThat(json.parse(expected))
                .isEqualTo(new CashCard(99L, 123.45, "LeudiX1"));
        assertThat(json.parseObject(expected).getId()).isEqualTo(99);
        assertThat(json.parseObject(expected).getAmount()).isEqualTo(123.45);
        assertThat(json.parseObject(expected).getOwner()).isEqualTo("LeudiX1");
    }

    /* Testing new features Data Contract */

    /*
     * Serialize the cashCards variable into JSON, then asserts that list.json
     * should contain the same data as the serialized cashCards variable
     */
    @Test
    void cashCardListSerializationTest() throws IOException {
        assertThat(jsonlist.write(cashCards)).isStrictlyEqualToJson("list.json");
    }

    @Test
    void cashCardListDeserializationTest() throws IOException {
        String expected = """
                [
                    { "id": 99, "amount": 123.45,"owner": "LeudiX1" },
                    { "id": 100, "amount": 100.50,"owner": "LeudiX1" },
                    { "id": 101, "amount": 325.33,"owner":"LeudiX1" }
                ]
                        """;
        assertThat(jsonlist.parse(expected)).isEqualTo(cashCards);
        assertThat(jsonlist.parseObject(expected)[0].getId()).isEqualTo(99);
        assertThat(jsonlist.parseObject(expected)[0].getAmount()).isEqualTo(123.45);
        assertThat(jsonlist.parseObject(expected)[0].getOwner()).isEqualTo("LeudiX1");
    }

}

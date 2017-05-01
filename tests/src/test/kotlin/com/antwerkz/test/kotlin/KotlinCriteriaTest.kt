/*
 * Copyright (C) 2012-2017 Justin Lee <jlee@antwerkz.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antwerkz.test.kotlin

import com.antwerkz.critter.kotlin.model.Address
import com.antwerkz.critter.kotlin.model.Invoice
import com.antwerkz.critter.kotlin.model.Item
import com.antwerkz.critter.kotlin.model.Person
import com.antwerkz.critter.test.kotlin.criteria.InvoiceCriteria
import com.antwerkz.critter.test.kotlin.criteria.PersonCriteria
import com.mongodb.MongoClient
import com.mongodb.WriteConcern
import org.mongodb.morphia.Datastore
import org.mongodb.morphia.Morphia
import org.testng.Assert
import org.testng.annotations.Test
import java.net.UnknownHostException
import java.time.LocalDateTime

@Test
class KotlinCriteriaTest {

    val datastore: Datastore by lazy {
        val mongo = MongoClient()
        val critter = mongo.getDB("critter")
        critter.dropDatabase()
        val morphia = Morphia()
        morphia.mapPackage("com.antwerkz")
        morphia.createDatastore(mongo, "critter")
    }

    fun invoice() {
        val john = com.antwerkz.critter.kotlin.model.Person("John", "Doe")
        datastore.save(john)
        datastore.save(com.antwerkz.critter.kotlin.model.Invoice(LocalDateTime.of(2012, 12, 21, 13, 15), john, com.antwerkz.critter.kotlin.model.Address("New York City", "NY", "10000"),
                Item("ball", 5.0), Item("skateboard", 17.35)))
        val jeff = com.antwerkz.critter.kotlin.model.Person("Jeff", "Johnson")
        datastore.save(jeff)
        datastore.save(com.antwerkz.critter.kotlin.model.Invoice(LocalDateTime.of(2006, 3, 4, 8, 7), jeff, com.antwerkz.critter.kotlin.model.Address("Los Angeles", "CA", "90210"),
                Item("movie", 29.95)))
        val sally = com.antwerkz.critter.kotlin.model.Person("Sally", "Ride")
        datastore.save(sally)
        datastore.save(com.antwerkz.critter.kotlin.model.Invoice(LocalDateTime.of(2007, 8, 16, 19, 27), sally, com.antwerkz.critter.kotlin.model.Address("Chicago", "IL", "99999"),
                Item("kleenex", 3.49), Item("cough and cold syrup", 5.61)))
        var invoiceCriteria = InvoiceCriteria(datastore)
        invoiceCriteria.person(john)
        val invoice = invoiceCriteria.query().get()
        val doe = datastore.createQuery<com.antwerkz.critter.kotlin.model.Invoice>(com.antwerkz.critter.kotlin.model.Invoice::class.java).filter("person =", john).get()
        Assert.assertEquals(invoice, doe)
        Assert.assertEquals(doe.person?.last, "Doe")
        Assert.assertEquals(invoice.person?.last, "Doe")
        val query = datastore.createQuery<com.antwerkz.critter.kotlin.model.Invoice>(com.antwerkz.critter.kotlin.model.Invoice::class.java).field("addresses.city").equal("Chicago").get()
        Assert.assertNotNull(query)
        invoiceCriteria = InvoiceCriteria(datastore)
        invoiceCriteria.addresses().city("Chicago")
        val critter = invoiceCriteria.query().get()
        Assert.assertNotNull(critter)
        Assert.assertEquals(critter, query)
    }

    @Test
    @Throws(UnknownHostException::class)
    fun updates() {
        val personCriteria = PersonCriteria(datastore)
        personCriteria.delete()
        personCriteria.first("Jim")
        personCriteria.last("Beam")

        val query = personCriteria.query()

        Assert.assertEquals(personCriteria.getUpdater()
                .age(30L)
                .updateAll().updatedCount, 0)

        Assert.assertEquals(personCriteria.getUpdater()
                .age(30L)
                .upsert().insertedCount, 1)

        val update = personCriteria.getUpdater().incAge().updateAll()
        Assert.assertEquals(update.updatedCount, 1)
        Assert.assertEquals(personCriteria.query().get().age!!.toLong(), 31L)

        Assert.assertNotNull(PersonCriteria(datastore).query().get().first)

        val delete = datastore.delete(query)
        Assert.assertEquals(delete.n, 1)
    }

    @Test(enabled = false) // waiting morphia issue #711
    fun updateFirst() {
        for (i in 0..99) {
            datastore.save(com.antwerkz.critter.kotlin.model.Person("First" + i, "Last" + i))
        }
        var criteria = PersonCriteria(datastore)
        criteria.last().contains("Last2")
        criteria.getUpdater()
                .age(1000L)
                .updateFirst()

        criteria = PersonCriteria(datastore)
        criteria.age(1000L)

        //    Assert.assertEquals(criteria.query().countAll(), 1);
    }

    @Test
    fun removes() {
        for (i in 0..99) {
            datastore.save(com.antwerkz.critter.kotlin.model.Person("First" + i, "Last" + i))
        }
        var criteria = PersonCriteria(datastore)
        criteria.last().contains("Last2")
        var result = criteria.getUpdater()
                .remove()
        Assert.assertEquals(result.n, 11)
        Assert.assertEquals(criteria.query().count(), 0)

        criteria = PersonCriteria(datastore)
        Assert.assertEquals(criteria.query().count(), 89)

        criteria = PersonCriteria(datastore)
        criteria.last().contains("Last3")
        result = criteria.getUpdater().remove(WriteConcern.MAJORITY)
        Assert.assertEquals(result.n, 11)
        Assert.assertEquals(criteria.query().count(), 0)
    }

    fun embeds() {
        var invoice = com.antwerkz.critter.kotlin.model.Invoice()
        invoice.date = LocalDateTime.now()
        var person = com.antwerkz.critter.kotlin.model.Person("Mike", "Bloomberg")
        datastore.save(person)
        invoice.person = person
        invoice.add(com.antwerkz.critter.kotlin.model.Address("New York City", "NY", "10036"))
        datastore.save(invoice)

        invoice = com.antwerkz.critter.kotlin.model.Invoice()
        invoice.date = LocalDateTime.now()
        person = com.antwerkz.critter.kotlin.model.Person("Andy", "Warhol")
        datastore.save(person)

        invoice.person = person
        invoice.add(com.antwerkz.critter.kotlin.model.Address("NYC", "NY", "10018"))
        datastore.save(invoice)

        val criteria1 = InvoiceCriteria(datastore)
        criteria1.addresses().city().order()
        Assert.assertEquals(criteria1.query().asList()[0].addresses!![0].city, "NYC")

        val criteria2 = InvoiceCriteria(datastore)
        criteria2.addresses().city().order(false)
        Assert.assertEquals(criteria2.query().asList()[0].addresses!![0].city, "New York City")
    }

    fun orQueries() {
        datastore.save(com.antwerkz.critter.kotlin.model.Person("Mike", "Bloomberg"))
        datastore.save(com.antwerkz.critter.kotlin.model.Person("Mike", "Tyson"))

        val query = datastore.createQuery<com.antwerkz.critter.kotlin.model.Person>(com.antwerkz.critter.kotlin.model.Person::class.java)
        query.or(
                query.criteria("last").equal("Bloomberg"),
                query.criteria("last").equal("Tyson")
        )

        val criteria = PersonCriteria(datastore)
        criteria.or(
                criteria.last("Bloomberg"),
                criteria.last("Tyson")
        )

        Assert.assertEquals(criteria.query().asList().size, 2)
        Assert.assertEquals(query.asList(), criteria.query().asList())
    }

    fun andQueries() {
        datastore.save(com.antwerkz.critter.kotlin.model.Person("Mike", "Bloomberg"))
        datastore.save(com.antwerkz.critter.kotlin.model.Person("Mike", "Tyson"))

        val query = datastore.createQuery<com.antwerkz.critter.kotlin.model.Person>(com.antwerkz.critter.kotlin.model.Person::class.java)
        query.and(
                query.criteria("first").equal("Mike"),
                query.criteria("last").equal("Tyson")
        )

        val criteria = PersonCriteria(datastore)
        criteria.and(
                criteria.first("Mike"),
                criteria.last("Tyson")
        )

        Assert.assertEquals(criteria.query().asList().size, 1)
        Assert.assertEquals(query.asList(), criteria.query().asList())
    }
}
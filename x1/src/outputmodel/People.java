package outputmodel;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "people")
@XmlAccessorType(XmlAccessType.FIELD)
public class People {

    @XmlElement(name = "person")
    private List<Person> persons = new ArrayList<>();

    public List<Person> getPersons() {
        return persons;
    }

    public void setPersons(List<Person> persons) {
        this.persons = persons;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Person {

        @XmlAttribute(name = "id", required = true)
        @XmlID
        private String id;

        @XmlAttribute(name = "gender")
        private String gender;

        @XmlElement(name = "firstName")
        private String firstName;

        @XmlElement(name = "lastName")
        private String lastName;

        @XmlElement(name = "mother")
        private Mother mother;

        @XmlElement(name = "father")
        private Father father;

        @XmlElement(name = "spouse")
        private Spouse spouse;

        @XmlElement(name = "children")
        private Children children;

        @XmlElement(name = "siblings")
        private Siblings siblings;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public Mother getMother() {
            return mother;
        }

        public void setMother(Mother mother) {
            this.mother = mother;
        }

        public Father getFather() {
            return father;
        }

        public void setFather(Father father) {
            this.father = father;
        }

        public Spouse getSpouse() {
            return spouse;
        }

        public void setSpouse(Spouse spouse) {
            this.spouse = spouse;
        }

        public Children getChildren() {
            return children;
        }

        public void setChildren(Children children) {
            this.children = children;
        }

        public Siblings getSiblings() {
            return siblings;
        }

        public void setSiblings(Siblings siblings) {
            this.siblings = siblings;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PersonRef {

        @XmlAttribute(name = "ref")
        @XmlIDREF
        private Person ref;

        @XmlValue
        private String name;

        public Person getRef() {
            return ref;
        }

        public void setRef(Person ref) {
            this.ref = ref;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Mother extends PersonRef {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Father extends PersonRef {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Spouse extends PersonRef {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Son extends PersonRef {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Daughter extends PersonRef {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Child extends PersonRef {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Brother extends PersonRef {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Sister extends PersonRef {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Sibling extends PersonRef {
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Children {

        @XmlElements({
                @XmlElement(name = "son", type = Son.class),
                @XmlElement(name = "daughter", type = Daughter.class),
                @XmlElement(name = "child", type = Child.class)
        })
        private List<PersonRef> children = new ArrayList<>();

        public List<PersonRef> getChildren() {
            return children;
        }

        public void setChildren(List<PersonRef> children) {
            this.children = children;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Siblings {

        @XmlElements({
                @XmlElement(name = "brother", type = Brother.class),
                @XmlElement(name = "sister", type = Sister.class),
                @XmlElement(name = "sibling", type = Sibling.class)
        })
        private List<PersonRef> siblings = new ArrayList<>();

        public List<PersonRef> getSiblings() {
            return siblings;
        }

        public void setSiblings(List<PersonRef> siblings) {
            this.siblings = siblings;
        }
    }
}
import outputmodel.People;
import outputmodel.People.*;

import java.util.*;

public class PersonConverter {

    public static People convert(Map<String, PersonInfo> personMap) {
        People people = new People();
        Map<String, Person> idToJaxbPerson = new HashMap<>();

        for (PersonInfo info : personMap.values()) {
            Person p = new Person();
            p.setId(info.id);
            p.setGender(info.gender);
            p.setFirstName(info.firstName);
            p.setLastName(info.lastName);

            idToJaxbPerson.put(info.id, p);
            people.getPersons().add(p);
        }

        for (PersonInfo info : personMap.values()) {
            Person p = idToJaxbPerson.get(info.id);

            if (!info.mothers.isEmpty()) {
                String m = info.mothers.iterator().next();
                if (!"UNKNOWN".equals(m)) {
                    Mother mother = new Mother();
                    if (isID(m)) {
                        mother.setRef(idToJaxbPerson.get(m));
                    } else {
                        mother.setName(m);
                    }
                    p.setMother(mother);
                }
            }

            if (!info.fathers.isEmpty()) {
                String f = info.fathers.iterator().next();
                if (!"UNKNOWN".equals(f)) {
                    Father father = new Father();
                    if (isID(f)) {
                        father.setRef(idToJaxbPerson.get(f));
                    } else {
                        father.setName(f);
                    }
                    p.setFather(father);
                }
            }

            if (info.spouse != null && !info.spouse.isEmpty() && !"NONE".equals(info.spouse)
                    && !"UNKNOWN".equals(info.spouse)) {
                Spouse spouse = new Spouse();
                if (isID(info.spouse)) {
                    spouse.setRef(idToJaxbPerson.get(info.spouse));
                } else {
                    spouse.setName(info.spouse);
                }
                p.setSpouse(spouse);
            }

            if (!info.sons.isEmpty() || !info.daughters.isEmpty() || !info.unknownChildren.isEmpty()) {
                Children children = new Children();
                List<PersonRef> childList = new ArrayList<>();

                for (String s : info.sons) {
                    Son son = new Son();
                    if (isID(s))
                        son.setRef(idToJaxbPerson.get(s));
                    else
                        son.setName(s);
                    childList.add(son);
                }
                for (String d : info.daughters) {
                    Daughter daughter = new Daughter();
                    if (isID(d))
                        daughter.setRef(idToJaxbPerson.get(d));
                    else
                        daughter.setName(d);
                    childList.add(daughter);
                }
                for (String c : info.unknownChildren) {
                    if (!"UNKNOWN".equals(c)) {
                        Child child = new Child();
                        if (isID(c))
                            child.setRef(idToJaxbPerson.get(c));
                        else
                            child.setName(c);
                        childList.add(child);
                    }
                }

                if (!childList.isEmpty()) {
                    children.setChildren(childList);
                    p.setChildren(children);
                }
            }

            if (!info.brothers.isEmpty() || !info.sisters.isEmpty() || !info.unknownSiblings.isEmpty()) {
                Siblings siblings = new Siblings();
                List<PersonRef> sibList = new ArrayList<>();

                for (String b : info.brothers) {
                    Brother brother = new Brother();
                    if (isID(b))
                        brother.setRef(idToJaxbPerson.get(b));
                    else
                        brother.setName(b);
                    sibList.add(brother);
                }

                for (String s : info.sisters) {
                    Sister sister = new Sister();
                    if (isID(s))
                        sister.setRef(idToJaxbPerson.get(s));
                    else
                        sister.setName(s);
                    sibList.add(sister);
                }

                for (String u : info.unknownSiblings) {
                    if (!"UNKNOWN".equals(u)) {
                        Sibling sibling = new Sibling();
                        if (isID(u))
                            sibling.setRef(idToJaxbPerson.get(u));
                        else
                            sibling.setName(u);
                        sibList.add(sibling);
                    }
                }

                if (!sibList.isEmpty()) {
                    siblings.setSiblings(sibList);
                    p.setSiblings(siblings);
                }
            }
        }

        return people;
    }

    private static boolean isID(String s) {
        return s != null && s.matches("P\\d+");
    }
}
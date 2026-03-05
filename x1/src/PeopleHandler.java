import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import java.util.*;

public class PeopleHandler extends DefaultHandler {
    private Map<String, PersonInfo> personMap = new HashMap<>();
    private PersonInfo currentPerson;
    private StringBuilder textBuffer = new StringBuilder();
    private Stack<String> elementStack = new Stack<>();
    private boolean inFullname = false;
    private String fullnameFirst = null;
    private String fullnameFamily = null;
    private boolean inChildren = false;
    private boolean inSiblings = false;
    private int genCounter = 0;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
        elementStack.push(qName);
        textBuffer.setLength(0);

        if (qName.equals("person")) {
            currentPerson = new PersonInfo();
            String id = atts.getValue("id");
            if (id != null)
                currentPerson.id = id;
            String name = atts.getValue("name");
            if (name != null)
                currentPerson.nameAttr = name;
            return;
        }

        String val = atts.getValue("value");
        if (val != null) {
            processValueAttribute(qName, val);
        }

        String idAttr = atts.getValue("id");
        if (idAttr != null) {
            if ((qName.equals("daughter") || qName.equals("son") || qName.equals("child")) && inChildren) {
                addChild(qName, idAttr, true);
            } else if ((qName.equals("brother") || qName.equals("sister")) && inSiblings) {
                addSibling(qName, idAttr, true);
            }
        }
        if (qName.equals("fullname")) {
            inFullname = true;
            fullnameFirst = null;
            fullnameFamily = null;
        } else if (qName.equals("children")) {
            inChildren = true;
        } else if (qName.equals("siblings")) {
            inSiblings = true;
            String siblingsVal = atts.getValue("val");
            if (siblingsVal != null) {
                for (String id : siblingsVal.split("\\s+")) {
                    if (!id.isEmpty())
                        currentPerson.siblingIDs.add(id);
                }
            }
        }
    }

    private void processValueAttribute(String qName, String val) {
        if (currentPerson == null)
            return;
        switch (qName) {
            case "firstname":
                currentPerson.firstName = val;
                break;
            case "surname":
                currentPerson.lastName = val;
                break;
            case "gender":
                currentPerson.gender = normalizeGender(val);
                break;
            case "parent":
                currentPerson.parents.add(val);
                break;
            case "father":
                currentPerson.fathers.add(val);
                break;
            case "mother":
                currentPerson.mothers.add(val);
                break;
            case "spouce":
            case "wife":
            case "husband":
                currentPerson.spouse = val;
                break;
            case "children-number":
                try {
                    currentPerson.childrenNumber = Integer.parseInt(val);
                } catch (NumberFormatException ignored) {
                }
                break;
            case "siblings-number":
                try {
                    currentPerson.siblingsNumber = Integer.parseInt(val);
                } catch (NumberFormatException ignored) {
                }
                break;
            case "id":
                if (currentPerson.id == null)
                    currentPerson.id = val;
                else if (!currentPerson.id.equals(val))
                    System.err.println("ID mismatch in same person entry: " + currentPerson.id + " vs " + val);
                break;
        }
    }

    private void addChild(String qName, String value, boolean isId) {
        if (currentPerson == null)
            return;
        String entry = value;
        if (qName.equals("son"))
            currentPerson.sons.add(entry);
        else if (qName.equals("daughter"))
            currentPerson.daughters.add(entry);
        else if (qName.equals("child"))
            currentPerson.unknownChildren.add(entry);
    }

    private void addSibling(String qName, String value, boolean isId) {
        if (currentPerson == null)
            return;
        String entry = value;
        if (qName.equals("brother"))
            currentPerson.brothers.add(entry);
        else if (qName.equals("sister"))
            currentPerson.sisters.add(entry);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String text = textBuffer.toString().trim();
        boolean hasText = !text.isEmpty();

        if (qName.equals("person")) {
            if (fullnameFirst != null || fullnameFamily != null) {
                if (fullnameFirst != null && currentPerson.firstName == null) {
                    currentPerson.firstName = fullnameFirst;
                }
                if (fullnameFamily != null && currentPerson.lastName == null) {
                    currentPerson.lastName = fullnameFamily;
                }
            }

            if (currentPerson.id == null) {
                currentPerson.id = "gen-" + (++genCounter);
                System.err.println("Generated ID: " + currentPerson.id);
            }

            PersonInfo existing = personMap.get(currentPerson.id);
            if (existing != null) {
                existing.merge(currentPerson);
            } else {
                personMap.put(currentPerson.id, currentPerson);
            }
            currentPerson = null;
            inFullname = false;
            fullnameFirst = null;
            fullnameFamily = null;
        } else if (hasText) {
            processTextElement(qName, text);
        }

        if (qName.equals("fullname")) {
            inFullname = false;
        } else if (qName.equals("children")) {
            inChildren = false;
        } else if (qName.equals("siblings")) {
            inSiblings = false;
        }

        elementStack.pop();
    }

    private void processTextElement(String qName, String text) {
        if (currentPerson == null)
            return;

        if (inFullname) {
            if (qName.equals("first")) {
                fullnameFirst = text;
                return;
            } else if (qName.equals("family")) {
                fullnameFamily = text;
                return;
            }
        }

        switch (qName) {
            case "firstname":
            case "first":
                currentPerson.firstName = text;
                break;
            case "surname":
            case "family-name":
            case "family":
                currentPerson.lastName = text;
                break;
            case "gender":
                currentPerson.gender = normalizeGender(text);
                break;
            case "mother":
                currentPerson.mothers.add(text);
                break;
            case "father":
                currentPerson.fathers.add(text);
                break;
            case "parent":
                currentPerson.parents.add(text);
                break;
            case "spouce":
            case "wife":
            case "husband":
                currentPerson.spouse = text;
                break;
            case "children-number":
                try {
                    currentPerson.childrenNumber = Integer.parseInt(text);
                } catch (NumberFormatException ignored) {
                }
                break;
            case "siblings-number":
                try {
                    currentPerson.siblingsNumber = Integer.parseInt(text);
                } catch (NumberFormatException ignored) {
                }
                break;
            case "id":
                if (currentPerson.id == null)
                    currentPerson.id = text;
                else if (!currentPerson.id.equals(text))
                    System.err.println("ID mismatch in same person entry: " + currentPerson.id + " vs " + text);
                break;
            case "daughter":
            case "son":
            case "child":
                if (inChildren)
                    addChild(qName, text, false);
                break;
            case "brother":
            case "sister":
                if (inSiblings)
                    addSibling(qName, text, false);
                break;
        }
    }

    private String normalizeGender(String g) {
        if (g == null)
            return null;
        g = g.trim().toLowerCase();
        if (g.equals("m") || g.equals("male"))
            return "male";
        if (g.equals("f") || g.equals("female"))
            return "female";
        return null;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        textBuffer.append(ch, start, length);
    }

    public Map<String, PersonInfo> getPersonMap() {
        return personMap;
    }

    public void printStats() {
        System.out.println("Total unique persons: " + personMap.size());
        int multiEntryCount = 0;
        for (PersonInfo p : personMap.values()) {
            if (p.mothers.size() > 1 || p.fathers.size() > 1 ||
                    p.sons.size() > 1 || p.daughters.size() > 1 ||
                    p.brothers.size() > 1 || p.sisters.size() > 1) {
                multiEntryCount++;
            }
        }
        System.out.println("Persons with merged data: " + multiEntryCount);
    }
}
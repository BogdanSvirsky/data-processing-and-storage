import java.util.HashSet;
import java.util.Set;

public class PersonInfo {
    String id;
    String firstName;
    String lastName;
    String gender;
    Set<String> mothers = new HashSet<>();
    Set<String> fathers = new HashSet<>();
    Set<String> parents = new HashSet<>();
    String spouse;
    Set<String> sons = new HashSet<>();
    Set<String> daughters = new HashSet<>();
    Set<String> unknownChildren = new HashSet<>();
    Set<String> brothers = new HashSet<>();
    Set<String> sisters = new HashSet<>();
    Set<String> siblingIDs = new HashSet<>();
    Set<String> unknownSiblings = new HashSet<>();
    Integer childrenNumber;
    Integer siblingsNumber;
    String nameAttr;

    public void merge(PersonInfo other) {
        if (other.id != null)
            this.id = other.id;

        if ((this.firstName == null || this.firstName.isEmpty()) && other.firstName != null
                && !other.firstName.trim().isEmpty())
            this.firstName = other.firstName;
        if ((this.lastName == null || this.lastName.isEmpty()) && other.lastName != null
                && !other.lastName.trim().isEmpty())
            this.lastName = other.lastName;
        if (this.gender == null && other.gender != null)
            this.gender = other.gender;
        if (this.spouse == null && other.spouse != null && !"NONE".equals(other.spouse))
            this.spouse = other.spouse;

        this.mothers.addAll(other.mothers);
        this.fathers.addAll(other.fathers);
        this.parents.addAll(other.parents);
        this.sons.addAll(other.sons);
        this.daughters.addAll(other.daughters);
        this.unknownChildren.addAll(other.unknownChildren);
        this.brothers.addAll(other.brothers);
        this.sisters.addAll(other.sisters);
        this.siblingIDs.addAll(other.siblingIDs);
        this.unknownSiblings.addAll(other.unknownSiblings);

        if (other.childrenNumber != null) {
            if (this.childrenNumber == null) {
                this.childrenNumber = other.childrenNumber;
            } else if (!this.childrenNumber.equals(other.childrenNumber)) {
                System.err.println("Warning: children-number mismatch for " + this.id +
                        ": " + this.childrenNumber + " vs " + other.childrenNumber);
            }
        }

        if (other.siblingsNumber != null) {
            if (this.siblingsNumber == null) {
                this.siblingsNumber = other.siblingsNumber;
            } else if (!this.siblingsNumber.equals(other.siblingsNumber)) {
                System.err.println("Warning: siblings-number mismatch for " + this.id +
                        ": " + this.siblingsNumber + " vs " + other.siblingsNumber);
            }
        }

        if (other.nameAttr != null)
            this.nameAttr = other.nameAttr;
    }
}
public class Validator {
    public static boolean validate(PersonInfo p) {
        boolean valid = true;

        int childCount = p.sons.size() + p.daughters.size() + p.unknownChildren.size();
        if (p.childrenNumber != null && p.childrenNumber != childCount) {
            System.err.println("WARNING: Children count mismatch for " + p.id +
                    ": declared=" + p.childrenNumber + ", actual=" + childCount);
            valid = false;
        }

        int siblingCount = p.brothers.size() + p.sisters.size() + p.unknownSiblings.size();
        if (p.siblingsNumber != null && p.siblingsNumber != siblingCount) {
            System.err.println("WARNING: Siblings count mismatch for " + p.id +
                    ": declared=" + p.siblingsNumber + ", actual=" + siblingCount);
            valid = false;
        }

        if (p.mothers.size() > 1) {
            System.err.println("WARNING: Multiple mothers for " + p.id + ": " + p.mothers);
        }
        if (p.fathers.size() > 1) {
            System.err.println("WARNING: Multiple fathers for " + p.id + ": " + p.fathers);
        }

        return valid;
    }
}
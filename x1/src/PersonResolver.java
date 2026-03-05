import java.util.*;

public class PersonResolver {
    public static void resolveNames(Map<String, PersonInfo> personMap) {
        Map<String, String> nameToId = new HashMap<>();
        Map<String, List<String>> duplicateNames = new HashMap<>();

        for (PersonInfo p : personMap.values()) {
            if (p.firstName != null && p.lastName != null) {
                String full = (p.firstName + " " + p.lastName).trim();
                if (nameToId.containsKey(full)) {
                    duplicateNames.computeIfAbsent(full, k -> new ArrayList<>()).add(nameToId.get(full));
                    duplicateNames.get(full).add(p.id);
                    nameToId.remove(full);
                } else if (!duplicateNames.containsKey(full)) {
                    nameToId.put(full, p.id);
                }
            }
        }

        for (PersonInfo p : personMap.values()) {
            resolveSet(p.mothers, nameToId, duplicateNames);
            resolveSet(p.fathers, nameToId, duplicateNames);
            resolveSet(p.parents, nameToId, duplicateNames);
            if (p.spouse != null && !isID(p.spouse) && !isSpecial(p.spouse)) {
                String resolved = nameToId.get(p.spouse);
                if (resolved != null)
                    p.spouse = resolved;
                else if (duplicateNames.containsKey(p.spouse))
                    System.err.println("Duplicate name for spouse: " + p.spouse);
            }
            resolveSet(p.sons, nameToId, duplicateNames);
            resolveSet(p.daughters, nameToId, duplicateNames);
            resolveSet(p.unknownChildren, nameToId, duplicateNames);
            resolveSet(p.brothers, nameToId, duplicateNames);
            resolveSet(p.sisters, nameToId, duplicateNames);
        }

        for (PersonInfo p : personMap.values()) {
            for (String sid : p.siblingIDs) {
                PersonInfo sib = personMap.get(sid);
                if (sib != null && sib.gender != null) {
                    if (sib.gender.equals("male"))
                        p.brothers.add(sid);
                    else if (sib.gender.equals("female"))
                        p.sisters.add(sid);
                    else
                        p.unknownSiblings.add(sid);
                } else {
                    p.unknownSiblings.add(sid);
                }
            }
            p.siblingIDs.clear();
        }
    }

    private static void resolveSet(Set<String> set, Map<String, String> nameToId,
            Map<String, List<String>> dup) {
        Set<String> toAdd = new HashSet<>();
        Set<String> toRemove = new HashSet<>();
        for (String s : set) {
            if (isID(s) || isSpecial(s))
                continue;
            String resolved = nameToId.get(s);
            if (resolved != null) {
                toRemove.add(s);
                toAdd.add(resolved);
            } else if (dup.containsKey(s)) {
                System.err.println("Duplicate name: " + s);
            }
        }
        set.removeAll(toRemove);
        set.addAll(toAdd);
    }

    private static boolean isID(String s) {
        return s != null && s.matches("P\\d+");
    }

    private static boolean isSpecial(String s) {
        return "UNKNOWN".equals(s) || "NONE".equals(s);
    }
}
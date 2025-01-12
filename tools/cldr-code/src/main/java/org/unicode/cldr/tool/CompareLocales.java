package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.SectionId;

import com.google.common.collect.ImmutableList;

/**
 * Tool to compare locales, generating tsv in the console.
 * TODO add regular parameter structure with output filename
 * @author markdavis
 *
 */
public class CompareLocales {
    /**
     * Supply list of locales as input args, like en en_001 en_GB
     * Only really useful if there is a shared non-root ancestor.
     * Skips all cases where
     * <ul>
     * <li>the values are identical in all the locales, and</li>
     * <li>at least one of the values is not inherited from a different path</li>
     * </ul>
     * @param args
     */
    public static void main(String[] args) {

        Arrays.sort(args);
        List<String> locales = ImmutableList.copyOf(Arrays.asList(args));
        List<CLDRFile> files = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        String prefix = "No\tSection\tPage\tHeader\tCode\t";
        for (String locale : locales) {
            System.out.print(prefix + locale);
            prefix = "\t";
            Factory factory = CLDRConfig.getInstance().getMainAndAnnotationsFactory();
            CLDRFile cldrFile = factory.make(locale, true);
            files.add(cldrFile);
            for (String path : cldrFile) {
                paths.add(path);
            }
        }
        showDiff(locales, (vi, vj) -> vi + " ≟ " + vj);
        System.out.println("\tConfig (2nd to last locale gets last value)");

        Set<PathHeader> sorted = new TreeSet<>();
        for (String path : paths) {
            sorted.add(PathHeader.getFactory().fromPath(path));
        }
        List<String> tempList = new ArrayList<>();

        Status status = new Status();
        int count = 0;

        for (PathHeader ph : sorted) {
            if (ph.getSectionId()== SectionId.Special) {
                continue;
            }
            tempList.clear();
            boolean atLocation = false;
            boolean someDifferent = false;
            String last = null;
            boolean isFirst = true;
            String originalPath = ph.getOriginalPath();
            for (CLDRFile cldrFile : files) {
                String stringValue = cldrFile.getStringValue(originalPath);
                tempList.add(stringValue);

                cldrFile.getSourceLocaleID(originalPath, status);
                if (status.pathWhereFound.equals(originalPath)) {
                    atLocation |= true;
                }
                if (!isFirst) {
                    someDifferent |= !Objects.equals(stringValue,last);
                }
                last = stringValue;
                isFirst = false;
            }
            if (!atLocation || !someDifferent) {
                continue;
            }

            System.out.print(++count + "\t" + ph);
            for (String value : tempList) {
                System.out.print("\t" + value);
            }
            showDiff(tempList, (vi, vj) -> (Objects.equals(vi,vj) ? "＝" : "≠"));

            //locale=da; action=addNew; new_path=//ldml/units/unitLength[@type="long"]/unit[@type="acceleration-g-force"]/unitPattern[@count="one"][@draft="provisional"]; new_value={0} g-kraft

            String penultimateLocale = locales.get(locales.size()-2);
            String ultimateValue = tempList.get(tempList.size()-1);
            System.out.println("\t"
                + "locale=" + penultimateLocale + "; "
                + "action=addNew; "
                + "new_path=" + originalPath + "; "
                + "new_value=" + ultimateValue);
        }
    }

    private static void showDiff(List<String> list, BiFunction<String,String,String> func) {
        for (int i = 0; i < list.size()-1; ++i) {
            String vi = list.get(i);
            for (int j = i + 1; j < list.size(); ++j) {
                String vj = list.get(j);
                System.out.print("\t" + func.apply(vi, vj));
            }
        }
    }
}

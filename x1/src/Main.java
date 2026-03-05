import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import outputmodel.People;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting XML processing...");

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        PeopleHandler handler = new PeopleHandler();

        System.out.println("Parsing people.xml...");
        saxParser.parse(new File("people.xml"), handler);

        System.out.println("Resolving names to IDs...");
        PersonResolver.resolveNames(handler.getPersonMap());

        System.out.println("Validating data consistency...");
        int validationErrors = 0;
        for (PersonInfo p : handler.getPersonMap().values()) {
            if (!Validator.validate(p)) {
                validationErrors++;
            }
        }
        System.out.println("Validation complete. Errors: " + validationErrors);

        System.out.println("Converting to JAXB objects...");
        People people = PersonConverter.convert(handler.getPersonMap());

        System.out.println("Marshalling to output.xml...");
        JAXBContext jaxbContext = JAXBContext.newInstance(People.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = sf.newSchema(new File("people.xsd"));
        marshaller.setSchema(schema);

        marshaller.marshal(people, new File("output.xml"));
        System.out.println("Success! Output written to output.xml");
    }
}
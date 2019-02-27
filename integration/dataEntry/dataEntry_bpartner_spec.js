
import { DataEntryGroup, DataEntrySubGroup, DataEntrySection } from '../../support/utils/dataEntryGroup';
import { DataEntryField, DataEntryListValue } from '../../support/utils/dataEntryField';

describe('Create bpartner with custom dataentry based tabs', function() {
    before(function() {
        // login before each test and open the flatrate conditions window
        cy.loginByForm();
    });

    it('Create bpartner with custom dataentry based tabs', function () {
        const timestamp = new Date().getTime(); // used in the document names, for ordering
        const dataEntryGroupName = `Group1 ${timestamp}`;

        const dataEntrySubGroup1Name = `SubGroup1-1 ${timestamp}`
        const dataEntrySection1Name = `Section1-1 ${timestamp}`
        const dataEntrySection2Name = `Section1-2 ${timestamp}`

        new DataEntryGroup
            .builder(dataEntryGroupName, 'Business Partner')
            .setTabName('Group1-Tab1')
            .setSeqNo(20)
            .setDescription(`Description of ${dataEntryGroupName}`)
            //.setActive(false) // you can set it to inactive, but then no subgroups or section can be added
            .addDataEntrySubGroup(new DataEntrySubGroup
                .builder(dataEntrySubGroup1Name)
                .setTabName('Group1-Tab1-SubTab1')
                .setDescription(`${dataEntrySubGroup1Name} - Description`)
                .setSeqNo(10)
                .build())
            .addDataEntrySection(new DataEntrySection
                .builder(dataEntrySection1Name)
                .setDescription(`${dataEntrySection1Name} - Description`)
                .setSeqNo(15)
                .build())
            .addDataEntrySection(new DataEntrySection
                .builder(dataEntrySection2Name)
                .setDescription(`${dataEntrySection2Name} - Description`)
                .setSeqNo(25)
                .build())
            .build()
            .apply();

        new DataEntryField
            .builder('Tab1-Field1', dataEntrySubGroup1Name)
            .setDataEntrySection(dataEntrySection1Name)
            .setDescription('Tab1-Field1 Description')
            .setMandatory(true)
            .setDataEntryRecordType('Yes-No')
            .setPersonalDataCategory('Personal')
            .setSeqNo(10)
            .build()
            .apply();

        new DataEntryField
            .builder('Tab1-Field2', dataEntrySubGroup1Name)
            .setDataEntrySection(dataEntrySection1Name)
            .setDescription('Tab1-Field2 Description')
            .setMandatory(false)
            .setDataEntryRecordType('Date')
            .setSeqNo(20)
            .build()
            .apply();

        new DataEntryField
                .builder('Tab1-Field3', dataEntrySubGroup1Name)
                .setDataEntrySection(dataEntrySection2Name)
                .setDescription('Tab1-Field3 Description')
                .setMandatory(true)
                .setDataEntryRecordType('List')
                .setSeqNo(30)
                .addDataEntryListValue(new DataEntryListValue
                    .builder('ListItem 1')
                    .setDescription('ListItem 1 Description')
                    .setSeqNo(20)
                    .build())
                .addDataEntryListValue(new DataEntryListValue
                    .builder('ListItem 2')
                    .setDescription('ListItem 2 Description')
                    .setSeqNo(10)
                    .build())
                .build()
                .apply();

        cy.visitWindow('123', 'NEW')
        cy.writeIntoStringField('CompanyName', `DataEntryBPartnerTestName ${timestamp}`)
        cy.selectTab(dataEntryGroupName)
        cy.selectTab(dataEntrySubGroup1Name)
    });


});
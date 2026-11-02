# FuelSurchargesCalculation
Transformation data on the calculated values of fuel surcharges from .xls to .txt for further loading in sap bw. 

The source data file is the Расчет надбавок на топливо.XLS. 

Its features

1)	Each line represents the calculated surcharges for a specific date and the characteristic [for which it is calculated] in the context of the fuel mark.
2)	The dates for which the calculated surcharges are presented are located in the merged cells. Each such cell occupies a number of rows equal to the number of characteristics for which surcharge are calculated.

   
When reading each line of the source file, it’s necessary.
1)	For each characteristic, along with the calculated values of fuel surcharges for each mark of fuel, it is necessary to bind the date value [from the merged cell].
   
The Apache POI is used to process the original Excel file.

The resulting file - FuelSurchargesCalculation.txt [a text file with semicolons as separators].


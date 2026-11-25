A tool used to convert jstack thread dumps into a format that [Kindling](https://forum.inductiveautomation.com/t/kindling-a-collection-of-tools-to-help-with-ignition/54689) can read for JVM thread dump analysis in [Ignition](https://inductiveautomation.com/). 

## Usage
For a single file. This will create a json file with the same name  
`java -jar jstackconverter.jar <input_file>`

For a directory. First arg is the input directory with all of the jstack thread dumps, second arg is the output directory
`java -jar jstackconverter.jar -r <input_path> <output_path>`

# SkidFuscatorMaven
## How to use?
1. Clone this repo and install it to local maven.
2. In your maven project of choice add it to pom.xml
```
<plugin>
    <groupId>me.chrommob.skidfuscatormaven</groupId>
    <artifactId>SkidFuscatorMaven</artifactId>
    <version>1.0.3</version>
</plugin>
```
3. In the project dir create skidfuscator folder and put skidfuscator.jar into it.
4. Compile your project.
5. Optional: Configure the depth you want to find dependencies in. You can do that by modifying the configuration of the plugin. The default value is 3 only increase if you errors regarding dependencies.
```
<plugin>
   <groupId>me.chrommob.skidfuscatormaven</groupId>
   <artifactId>SkidFuscatorMaven</artifactId>
   <version>1.0.3</version>
   <configuration>
      <maxDepth>4</maxDepth>
   </configuration>
</plugin>
```
6. Run skidfuscate task.
7. Enjoy!

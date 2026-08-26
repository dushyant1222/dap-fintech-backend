
import re

with open("pom.xml", "r") as f:
    text = f.read()

bad_exclude = """<exclude>
\t\t\t\t\t\t\t<groupId>org.projectlombok</groupId>
\t\t\t\t\t\t\t<artifactId>lombok</artifactId>
\t\t\t\t\t\t\t\t\t<version>1.18.30</version>
\t\t\t\t\t\t</exclude>"""

good_exclude = """<exclude>
\t\t\t\t\t\t\t<groupId>org.projectlombok</groupId>
\t\t\t\t\t\t\t<artifactId>lombok</artifactId>
\t\t\t\t\t\t</exclude>"""

if bad_exclude in text:
    text = text.replace(bad_exclude, good_exclude)
else:
    print("Exact bad exclude block not found, doing regex replacement...")
    # use a regex to find the exclude block and remove the version line
    # Match <exclude> followed by lombok and version
    text = re.sub(
        r"(<exclude>\s*<groupId>org\.projectlombok</groupId>\s*<artifactId>lombok</artifactId>)\s*<version>1\.18\.30</version>(\s*</exclude>)",
        r"\1\2",
        text
    )

with open("pom.xml", "w") as f:
    f.write(text)


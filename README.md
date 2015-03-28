# ReflectiveToStringHelper

You may have heard of ToStringHelper, the awesome tool in Google's Guava project (MoreObjects#toStringHelper). This isn't that.

It's pretty similar, though.

ReflectiveToStringHelper (or just RTSH – arr tush) uses Java's reflection capabilities to pick up the fields of any object and generate a toString from them, whether they're private, package-local, protected, or public. Access to the fields doesn't matter.

Rather than drone on about how it works, take a look at some examples!

## Examples

```java
public class Person {
    public String firstName = "Joe";
    public String lastName = "Schmoe";
    public float age = 23.4F;
    public final float height = 6.083F; // Joe is done growing. He's 6'1".
    protected int numberOfFriends = 3;
    protected int numberOfExes = 2;
    private boolean inARelationship = false;
    private boolean thinksHeIsGreat = true;
    private volatile int timesCried = 100;

    // As you'll see, we only work with fields. This never shows up anywhere.
    public String getLastSaidSentence() {
        return "Howdy!";
    }
}

final Person joe = new Person();
// 1. Let's only print what someone in the public would know about Joe.
// 1. Person{firstName=Joe,lastName=Schmoe,age=23.4,height=6.083}
log(
    ReflectiveToStringHelper.of(
        joe
    )
);
// 2. How about his friends?
// 2. Person{firstName=Joe,lastName=Schmoe,age=23.4,height=6.083,numberOfFriends=3,numberOfExes=2}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).protecteds(true)
    )
);
// 3. The things only Joe knows
// 3. Person{inARelationship=false,thinksHeIsGreat=true,timesCried=100}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().privates(true)
    )
);
// 4. What someone in the public knows and ONLY his amount of friends
// 4. Person{firstName=Joe,lastName=Schmoe,age=23.4,height=6.083,numberOfFriends=3}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).ensure("numberOfFriends")
    )
);
// 5. Joe wants people to think he's in a relationship
// 5. Person{firstName=Joe,lastName=Schmoe,age=23.4,height=6.083,inARelationship=true}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).map("thinksHeIsGreat", "inARelationship").ensure("thinksHeIsGreat")
    )
);
// 6. Joe is a really open guy, except about his exes.
// 6. Person{firstName=Joe,lastName=Schmoe,age=23.4,height=6.083,numberOfFriends=3,inARelationship=false,thinksHeIsGreat=true,timesCried=100}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).privates(true).packages(true).protecteds(true).exclude("numberOfExes")
    )
);
// 7. Joe isn't really a numbers guy with his family.
// 7. Person{firstName=Joe,lastName=Schmoe}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).protecteds(true).exclude(Integer.TYPE).exclude(Float.TYPE)
    )
);
// 8. Joe wants everyone to know the private on/off parts of his life, but nothing else.
// 8. Person{inARelationship=false,thinksHeIsGreat=true}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().ensure(Boolean.TYPE)
    )
);
// 9. The only number Joe really likes is his height.
// 9. Person{height=6.083}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().ensure("height", Float.TYPE)
    )
);
// 10. Joe doesn't want people to know the fixed parts of his life.
// 10. Person{firstName=Joe,lastName=Schmoe,age=23.4}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().publics(true).keepFinals(false)
    )
);
// 11. Joe just can't control what makes him cry. He doesn't want people to know.
// 11. Person{inARelationship=false,thinksHeIsGreat=true}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().privates(true).keepVolatiles(false)
    )
);
// 12. Joe doesn't want to appear arrogant or weak!
// 12. Person{inARelationship=false}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().privates(true).exclude("thinksHeIsGreat").exclude("timesCried")
    )
);
// 13. Joe wants to share all of the positive aspects of his life.
// 13. Person{thinksHeIsGreat=true}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().ensureValue(true)
    )
);
// 14. When Joe is single, he's proud of it. He will only share his relationship status if he's single.
// 14. Person{inARelationship=false}
log(
    ReflectiveToStringHelper.of(
        joe,
        Include.create().ensure("inARelationship", Boolean.TYPE, false)
    )
);
```
